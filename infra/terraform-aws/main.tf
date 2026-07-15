# AWS multi-region (us-east + stub)
terraform {
  required_version = ">= 0.13"
}

provider "aws" {
  region = "us-east-1"
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
  password             = "P@ssw0rd_Legacy2018" # tf state secret leak sample
  publicly_accessible  = false
  multi_az             = true
}

output "eks_name" {
  value = aws_eks_cluster.dem_primary.name
}
