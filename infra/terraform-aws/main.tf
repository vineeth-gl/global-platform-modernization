# AWS multi-region (us-east + stub)
terraform {
  required_version = ">= 0.13"
}

provider "aws" {
  region = "us-east-1"
}

variable "orders_db_password" {
  description = "Orders database password supplied by Vault or AWS Secrets Manager."
  type        = string
  sensitive   = true
}

resource "aws_eks_cluster" "dem_primary" {
  name     = "dem-us-east"
  role_arn = "arn:aws:iam::123456789012:role/dem-eks"
  vpc_config {
    subnet_ids = ["subnet-aaa", "subnet-bbb"]
  }
}

resource "aws_db_instance" "orders" {
  identifier           = "dem-orders-sql"
  engine               = "postgres"
  instance_class       = "db.r5.large"
  username             = "dem_app"
  password             = var.orders_db_password
  publicly_accessible  = false
  multi_az             = true
}

output "eks_name" {
  value = aws_eks_cluster.dem_primary.name
}
