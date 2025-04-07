# terraform/outputs.tf

output "vpc_id" {
  description = "The ID of the VPC"
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "The IDs of the public subnets"
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "The IDs of the private subnets"
  value       = module.vpc.private_subnet_ids
}

output "db_endpoint" {
  description = "The endpoint of the database"
  value       = module.rds.db_endpoint
  sensitive   = true
}

output "elasticsearch_endpoint" {
  description = "The endpoint of the Elasticsearch domain"
  value       = module.elasticsearch.elasticsearch_endpoint
}

output "ec2_public_ip" {
 description = "The public IP of the EC2 instance"
 value       = module.ec2.ec2_public_ip
}