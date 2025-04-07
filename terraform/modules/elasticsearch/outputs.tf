# terraform/modules/elasticsearch/outputs.tf

output "elasticsearch_endpoint" {
  description = "Endpoint of the Elasticsearch domain"
  value       = aws_elasticsearch_domain.main.endpoint
}

output "elasticsearch_domain_id" {
  description = "ID of the Elasticsearch domain"
  value       = aws_elasticsearch_domain.main.domain_id
}

output "elasticsearch_domain_name" {
  description = "Name of the Elasticsearch domain"
  value       = aws_elasticsearch_domain.main.domain_name
}