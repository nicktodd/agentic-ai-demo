output "alb_dns_name" {
  description = "Public DNS name of the load balancer."
  value       = aws_lb.app.dns_name
}

output "app_url" {
  description = "Base URL of the deployed app."
  value       = "http://${aws_lb.app.dns_name}"
}

output "health_url" {
  description = "Actuator health endpoint - open this to check the deploy worked."
  value       = "http://${aws_lb.app.dns_name}/actuator/health"
}

output "ecr_repository_url" {
  description = "Push images here (docker tag <image> <this>:<tag>)."
  value       = aws_ecr_repository.app.repository_url
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.app.name
}

output "ecs_service_name" {
  value = aws_ecs_service.app.name
}

output "task_execution_role_arn" {
  description = "Jenkins needs iam:PassRole on this role to register task definitions."
  value       = aws_iam_role.task_execution.arn
}
