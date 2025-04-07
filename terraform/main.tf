# terraform/main.tf

provider "aws" {
  region = var.aws_region
}

module "vpc" {
  source = "./modules/vpc"

  vpc_cidr_block       = var.vpc_cidr_block
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  availability_zones   = var.availability_zones
  project_name         = var.project_name
}

module "rds" {
  source = "./modules/rds"

  project_name          = var.project_name
  vpc_id                 = module.vpc.vpc_id
  db_subnet_group_name   = module.vpc.db_subnet_group_name
  db_security_group_id   = module.vpc.db_security_group_id
  db_name                = var.db_name
  db_username            = var.db_username
  db_password            = var.db_password
  db_instance_class      = var.db_instance_class
}

module "elasticsearch" {
  source = "./modules/elasticsearch"

  vpc_id                = module.vpc.vpc_id
  private_subnet_ids    = module.vpc.private_subnet_ids
  elasticsearch_sg_id   = module.vpc.elasticsearch_sg_id
  domain_name           = var.elasticsearch_domain_name
  instance_type         = var.elasticsearch_instance_type
}

module "ec2" {
  source = "./modules/ec2"

  vpc_id                = module.vpc.vpc_id
  public_subnet_id      = module.vpc.public_subnet_ids[0]
  app_security_group_id = module.vpc.app_security_group_id
  key_name              = var.key_name
  instance_type         = var.ec2_instance_type
  internet_gateway_id   = module.vpc.internet_gateway_id
  project_name          = var.project_name
}