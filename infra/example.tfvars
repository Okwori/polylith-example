# Copy to staging.tfvars / prod.tfvars and fill in real values.
# These files are gitignored (except this example).

environment       = "staging"
aws_region        = "us-east-1"
api_gateway_id    = "abc1234def"   # from AWS Console → API Gateway → APIs
api_gateway_stage = "$default"
mulog_log_group   = "/pringwa/service"
alert_email       = "oncall@example.com"

# Optional overrides
waf_rate_limit             = 2000
alarm_error_rate_threshold = 10
alarm_latency_threshold_ms = 3000
