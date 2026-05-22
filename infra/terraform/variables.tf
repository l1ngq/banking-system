variable "vm_box" {
  type    = string
  default = "ubuntu/jammy64"
}

variable "vm_name_prefix" {
  type    = string
  default = "k3s"
}

variable "network_prefix" {
  type    = string
  default = "192.168.56"
}

variable "master_ip_last_octet" {
  type    = number
  default = 10
}

variable "worker_ip_start" {
  type    = number
  default = 11
}

variable "worker_count" {
  type    = number
  default = 2
}

variable "master_resources" {
  type = object({
    memory = number
    cpus   = number
  })
  default = {
    memory = 4096
    cpus   = 2
  }
}

variable "worker_resources" {
  type = object({
    memory = number
    cpus   = number
  })
  default = {
    memory = 8192
    cpus   = 4
  }
}

variable "disable_synced_folder" {
  type    = bool
  default = true
}
