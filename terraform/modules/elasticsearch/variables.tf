# terraform/modules/elasticsearch/variables.tf

variable "project_name" {
  description = "Name of the project"
  type        = string
  default     = "cse460-llm-assistant"
}

variable "vpc_id" {
  description = "ID of the VPC"
  type        = string
}

variable "private_subnet_ids" {
  description = "IDs of private subnets"
  type        = list(string)
}

variable "elasticsearch_sg_id" {
  description = "ID of the Elasticsearch security group"
  type        = string
}

variable "domain_name" {
  description = "Domain name for Elasticsearch"
  type        = string
}

variable "instance_type" {
  description = "Instance type for Elasticsearch"
  type        = string
  default     = "t3.small.elasticsearch"
}