# Application Load Balancer: the public front door for the app.

resource "aws_lb" "app" {
  name               = "${var.name}-alb" # the Jenkinsfile looks the ALB up by this name
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = data.aws_subnets.default.ids
}

# Fargate tasks get their own IP, so the target group type is "ip" (not "instance").
resource "aws_lb_target_group" "app" {
  name        = "${var.name}-tg"
  port        = var.container_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = data.aws_vpc.default.id

  # How long to keep sending traffic to an old task during a rolling deploy
  # before killing it.  Short, because the demo app has no long requests.
  deregistration_delay = 30

  health_check {
    path                = "/actuator/health" # Spring Boot Actuator
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

# Plain HTTP on port 80.  HTTPS would need a certificate + a DNS name, which is
# out of scope for a sandbox demo.
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.app.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}
