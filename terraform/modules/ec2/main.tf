# Amazon Linux 2023 AMI lookup
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# EC2 Instance
resource "aws_instance" "app_server" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.instance_type
  key_name               = var.key_name
  subnet_id              = var.public_subnet_id
  vpc_security_group_ids = [var.app_security_group_id]

  root_block_device {
    volume_size = 30
    volume_type = "gp2"
    encrypted   = true
  }

  user_data = <<-EOF
              #!/bin/bash
              # Update system
              dnf update -y

              # Install necessary packages
              dnf install -y docker git

              # Start and enable Docker service
              systemctl start docker
              systemctl enable docker

              # Install Docker Compose
              curl -L "https://github.com/docker/compose/releases/download/v2.23.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
              chmod +x /usr/local/bin/docker-compose

              # Add ec2-user to docker group
              usermod -aG docker ec2-user

              # Install Java 17
              dnf install -y java-17-amazon-corretto-devel

              # Install Maven
              dnf install -y maven

              # Create app directory
              mkdir -p /app
              chown ec2-user:ec2-user /app

              echo "Setup complete!"
              EOF

  tags = {
    Name = "${var.project_name}-app-server"
  }
}

# Elastic IP for EC2 Instance
resource "aws_eip" "app_server" {
  domain     = "vpc"
  instance   = aws_instance.app_server.id
  depends_on = [var.internet_gateway_id]

  tags = {
    Name = "${var.project_name}-app-server-eip"
  }
}