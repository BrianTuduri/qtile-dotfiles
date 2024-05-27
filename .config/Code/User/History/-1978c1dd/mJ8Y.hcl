terraform {
  extra_arguments "vars" {
    commands = ["apply", "plan", "import", "push", "refresh", "destroy"]
    arguments = [
      "-lock=false",
      "-auto-approve",
      ]
  }
}

generate "backend" {
  path      = "backend.tg.tf"
  if_exists = "overwrite_terragrunt"
  contents = <<EOF
terraform {
  backend "http" {
    address        = "${get_env("TF_STATE_ADDRESS")}/terraform.tfstate"
    #lock_address   = "${get_env("TF_STATE_ADDRESS")}/terraform.tfstate.lock"
    #unlock_address = "${get_env("TF_STATE_ADDRESS")}/terraform.tfstate.lock"
    username       = "${get_env("TF_USERNAME")}"
    password       = "${get_env("TF_PASSWORD")}"
    lock_method    = "POST" 
    unlock_method  = "DELETE" 
    retry_wait_min = "5"
  }
  required_providers {
    rancher2 = {
      source = "rancher/rancher2"
    }
  }
}
provider "helm" {
  kubernetes {
    config_path = "${get_env("KUBECONFIG_PATH")}"
  }
}
EOF
}