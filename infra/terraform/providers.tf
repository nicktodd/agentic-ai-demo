provider "aws" {
  region = var.region

  # Every resource gets these tags.  Handy for finding (and deleting!) demo
  # resources afterwards, and for cost reports in the sandbox account.
  default_tags {
    tags = {
      project    = "agentic-ai-demo"
      owner      = var.owner
      ephemeral  = "true"
      managed_by = "terraform"
    }
  }
}
