# Container registry.  Jenkins pushes images here; ECS pulls from here.
resource "aws_ecr_repository" "app" {
  name = var.name

  # Allow `terraform destroy` even when images are still in the repo.
  force_delete = true

  # Scan each pushed image for known CVEs (results in the ECR console).
  image_scanning_configuration {
    scan_on_push = true
  }

  # "MUTABLE" lets Jenkins re-point the "latest" tag at every build.
  image_tag_mutability = "MUTABLE"
}

# Keep only the 10 most recent images so the repo doesn't grow forever.
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = { type = "expire" }
    }]
  })
}
