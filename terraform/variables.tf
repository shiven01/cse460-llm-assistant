# terraform/variables.tf

variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Name of the project"
  type        = string
  default     = "cse460-llm-assistant"
}

variable "vpc_cidr_block" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
  default     = ["10.0.3.0/24", "10.0.4.0/24"]
}

variable "availability_zones" {
  description = "Availability zones for subnets"
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "your_ip" {
  description = "Your IP address for SSH access (CIDR notation)"
  type        = string
  default     = "0.0.0.0/0"  # Replace with your IP in terraform.tfvars
}

# Database variables
variable "db_name" {
  description = "Name of the database"
  type        = string
  default     = "llm_assistant"
}

variable "db_username" {
  description = "Username for database access"
  type        = string
  default     = "dbadmin"
}

variable "db_password" {
  description = "Password for database access"
  type        = string
  sensitive   = true
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

# Elasticsearch variables
variable "elasticsearch_domain_name" {
  description = "Domain name for Elasticsearch"
  type        = string
  default     = "llm-assistant-es"
}

variable "elasticsearch_instance_type" {
  description = "Instance type for Elasticsearch"
  type        = string
  default     = "t3.small.elasticsearch"
}

# EC2 variables
variable "key_name" {
  description = "Name of key pair for SSH access"
  type        = string
}

variable "ec2_instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t2.micro"
}