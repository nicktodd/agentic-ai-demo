variable "region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "eu-west-2" # London
}

variable "name" {
  description = "Base name used for every resource (ECR repo, cluster, service, ALB ...)."
  type        = string
  default     = "portfolio-valuation"
}

variable "owner" {
  description = "Who owns these resources (tag only). Set it in terraform.tfvars."
  type        = string
}

variable "image_tag" {
  description = "Image tag the ECS task definition points at on first apply. Jenkins replaces it with the git SHA on each deploy."
  type        = string
  default     = "latest"
}

variable "container_port" {
  description = "Port the Spring Boot app listens on inside the container."
  type        = number
  default     = 8080
}

variable "cpu" {
  description = "Fargate CPU units (1024 = 1 vCPU). 512 is the smallest that pairs with 1 GB."
  type        = number
  default     = 512
}

variable "memory" {
  description = "Fargate memory in MiB."
  type        = number
  default     = 1024
}

variable "desired_count" {
  description = "Number of running tasks. 1 is enough for a demo; set 0 to create the service without starting anything."
  type        = number
  default     = 1
}
