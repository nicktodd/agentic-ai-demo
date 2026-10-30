# Networking.
#
# To keep the demo cheap and simple we reuse the account's DEFAULT VPC and its
# public subnets.  A real environment would run the tasks in private subnets
# behind a NAT gateway (which alone costs ~$1/day) and only expose the ALB.

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# Security group for the load balancer: anyone on the internet may reach port 80.
resource "aws_security_group" "alb" {
  name        = "${var.name}-alb-sg"
  description = "ALB: allow HTTP from the internet"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "HTTP from anywhere"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Security group for the ECS tasks: ONLY the ALB may talk to the container port.
resource "aws_security_group" "task" {
  name        = "${var.name}-task-sg"
  description = "ECS tasks: allow app port from the ALB only"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "App port from the ALB"
    from_port       = var.container_port
    to_port         = var.container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # Outbound is needed to pull the image from ECR and ship logs to CloudWatch.
  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
