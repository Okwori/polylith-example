terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    # Override at init time:
    #   terraform init \
    #     -backend-config="bucket=<your-tfstate-bucket>" \
    #     -backend-config="key=pringwa-service/<env>/terraform.tfstate" \
    #     -backend-config="region=us-east-1"
    region = "us-east-1"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "pringwa-service"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}
