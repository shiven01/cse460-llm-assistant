# terraform/modules/rds/main.tf

resource "aws_db_instance" "main" {
  identifier              = "${var.project_name}-db"
  allocated_storage       = 20
  storage_type            = "gp2"
  engine                  = "postgres"
  engine_version          = "14.17"
  instance_class          = var.db_instance_class
  db_name                 = var.db_name
  username                = var.db_username
  password                = var.db_password
  db_subnet_group_name    = var.db_subnet_group_name
  vpc_security_group_ids  = [var.db_security_group_id]
  publicly_accessible     = false
  skip_final_snapshot     = true
  backup_retention_period = 7

  tags = {
    Name = "${var.project_name}-db"
  }
}