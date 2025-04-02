# terraform/modules/vpc/variables.tf

variable "vpc_cidr_block" {
  description = "CIDR block for VPC"
  type        = string
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets"
  type        = list(string)
}

variable "availability_zones" {
  description = "Availability zones for subnets"
  type        = list(string)
}

variable "project_name" {
  description = "Name of the project"
  type        = string
}

variable "your_ip" {
  description = "Your IP address for SSH access (CIDR notation)"
  type        = string
  default     = "0.0.0.0/0"  # Replace with your IP in terraform.tfvars
}