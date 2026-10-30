# ECS on Fargate: runs the container without us managing any EC2 instances.

resource "aws_ecs_cluster" "app" {
  name = "${var.name}-cluster" # matches ECS_CLUSTER in the Jenkinsfile

  # Container Insights adds detailed metrics but also cost; off for the demo.
  setting {
    name  = "containerInsights"
    value = "disabled"
  }
}

# Where the container's stdout/stderr ends up.  7 days is plenty for a demo.
resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.name}"
  retention_in_days = 7
}

# ---------------------------------------------------------------------------
# IAM: the *execution* role is what ECS itself uses to pull the image from ECR
# and write logs to CloudWatch.  (A separate *task* role would be for the app's
# own AWS API calls; this app makes none, so it isn't needed.)
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "ecs_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "task_execution" {
  name               = "${var.name}-task-execution-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

resource "aws_iam_role_policy_attachment" "task_execution" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ---------------------------------------------------------------------------
# Task definition: "what to run".  Jenkins registers a new revision of this on
# every deploy with the new image tag; Terraform only creates the first one.
# ---------------------------------------------------------------------------
resource "aws_ecs_task_definition" "app" {
  family                   = var.name
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc" # required for Fargate; each task gets its own ENI/IP
  cpu                      = var.cpu
  memory                   = var.memory
  execution_role_arn       = aws_iam_role.task_execution.arn

  container_definitions = jsonencode([
    {
      name      = var.name # the Jenkinsfile edits containerDefinitions[0], so keep a single container
      image     = "${aws_ecr_repository.app.repository_url}:${var.image_tag}"
      essential = true

      portMappings = [{
        containerPort = var.container_port
        protocol      = "tcp"
      }]

      # Spring profile etc. could go here.  Secrets would use "secrets" + SSM/Secrets Manager.
      environment = [
        { name = "JAVA_OPTS", value = "" }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.app.name
          "awslogs-region"        = var.region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      # Container-level health check (in addition to the ALB's).  Uses curl,
      # which the Dockerfile installs.  startPeriod gives Spring Boot time to boot.
      healthCheck = {
        command     = ["CMD-SHELL", "curl -fsS http://localhost:${var.container_port}/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])
}

# ---------------------------------------------------------------------------
# Service: "keep N copies of the task running and register them with the ALB".
# ---------------------------------------------------------------------------
resource "aws_ecs_service" "app" {
  name            = "${var.name}-service" # matches ECS_SERVICE in the Jenkinsfile
  cluster         = aws_ecs_cluster.app.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = data.aws_subnets.default.ids
    security_groups = [aws_security_group.task.id]
    # Default-VPC subnets are public and there is no NAT, so the task needs a
    # public IP to reach ECR and CloudWatch.  Traffic *in* is still restricted
    # to the ALB by the security group.
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = var.name
    container_port   = var.container_port
  }

  # Give the app time to start before ECS trusts the ALB health check.
  health_check_grace_period_seconds = 60

  # After the first apply, Jenkins owns the task definition revision (it
  # registers a new one per deploy) and may scale the service.  Without this,
  # the next `terraform apply` would roll the service BACK to the Terraform
  # version.  ignore_changes tells Terraform "don't fight Jenkins over these".
  lifecycle {
    ignore_changes = [task_definition, desired_count]
  }

  # The listener must exist before the service tries to register targets.
  depends_on = [aws_lb_listener.http]
}
