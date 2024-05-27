# Provider conf
terraform {
  required_providers {
    kubernetes = {
      source = "hashicorp/kubernetes"
    }
    rancher2 = {
      source = "rancher/rancher2"
    }
  }
}
variable "metallb_ip_range" { type = string }
variable "cluster_name" { type = string }
//variable "cluster_id" { type = string }
variable "global_registry" { type = string }
variable "service_account_name" { type = string }
locals {
  system_namespaces = {
    "terraform-states" : "terraform-states"
    "longhorn-system" : "longhorn-system"
    "metallb-system" : "metallb-system"
  }
}

## Setup namespaces and service account 
data "rancher2_project" "system_ns" {
  cluster_id = var.cluster_id
  name       = "System"
}
resource "rancher2_namespace" "system-namespaces" {
  for_each   = local.system_namespaces
  name       = each.key
  project_id = data.rancher2_project.system_ns.id
}

## Bind permissions to service account
resource "rancher2_cluster_role_template_binding" "jenkinsbinding" {
  name             = var.service_account_name
  cluster_id       = var.cluster_id # faltaria iterar
  role_template_id = "cluster-owner"
  #user_id          = rancher2_user.jenkins_service_account.id
}

## Add features
module "networking" {
  namespace        = rancher2_namespace.system-namespaces["metallb-system"].name
  source           = "./features/networking"
  global_registry  = var.global_registry
  metallb_ip_range = var.metallb_ip_range
}
module "ingress" {
  depends_on   = [module.networking]
  source       = "./features/ingress"
  namespace    = "kube-system"
  crt_location = ""
  key_location = ""
}
module "storage" {
  source = "./features/storage"
}