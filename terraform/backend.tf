terraform {
  backend "s3" {
    bucket  = "finanzio-backend"
    key     = "finanzio"
    region  = "eu-central-1"
    profile = "gabro"
  }
}
