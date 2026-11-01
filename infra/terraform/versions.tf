# Pin tool and provider versions so every laptop in the room gets the same behaviour.
# Written for OpenTofu (>= 1.6); plain Terraform >= 1.5 reads the same files and state.
# The block is still called "terraform" in OpenTofu for compatibility.
terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # State is kept locally (terraform.tfstate) because this is a short-lived sandbox.
  # A real project would use an S3 backend with state locking.
}
