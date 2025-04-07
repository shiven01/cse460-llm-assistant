variable "project_name" {
  description = "Name of the project"
  type        = string
  default     = "cse460-llm-assistant"
}

variable "vpc_id" {
  description = "ID of the VPC"
  type        = string
}

variable "public_subnet_id" {
  description = "ID of the public subnet"
  type        = string
}

variable "app_security_group_id" {
  description = "ID of the application security group"
  type        = string
}

variable "key_name" {
  description = "Name of key pair for SSH access"
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t2.micro"
}

variable "internet_gateway_id" {
  description = "ID of the Internet Gateway"
  type        = string
}