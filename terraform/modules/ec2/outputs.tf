output "ec2_instance_id" {
  description = "ID of the EC2 instance"
  value       = aws_instance.app_server.id
}

output "ec2_public_ip" {
  description = "Public IP of the EC2 instance"
  value       = aws_eip.app_server.public_ip
}

output "ec2_public_dns" {
  description = "Public DNS of the EC2 instance"
  value       = aws_instance.app_server.public_dns
}