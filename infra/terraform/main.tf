terraform {
  required_version = ">= 1.15.4"
  required_providers {
    local = {
      source  = "hashicorp/local"
      version = "~> 2.9.0"
    }
    null = {
      source  = "hashicorp/null"
      version = "~> 3.3.0"
    }
  }
}

locals {
  nodes = concat(
    [
      {
        name   = "${var.vm_name_prefix}-master"
        ip     = "${var.network_prefix}.${var.master_ip_last_octet}"
        memory = var.master_resources.memory
        cpus   = var.master_resources.cpus
      }
    ],
    [
      for idx in range(var.worker_count) : {
        name   = "${var.vm_name_prefix}-worker-${idx + 1}"
        ip     = "${var.network_prefix}.${var.worker_ip_start + idx}"
        memory = var.worker_resources.memory
        cpus   = var.worker_resources.cpus
      }
    ]
  )

  vagrant_nodes = join("\n\n", [
    for node in local.nodes : <<-NODE
      config.vm.define "${node.name}" do |vm|
        vm.vm.hostname = "${node.name}"
        vm.vm.network "private_network", ip: "${node.ip}"

        vm.vm.provider "virtualbox" do |vb|
          vb.memory = ${node.memory}
          vb.cpus = ${node.cpus}
          vb.name = "${node.name}"
        end
      end
    NODE
  ])

  vagrantfile_content = <<-EOT
    Vagrant.configure("2") do |config|
      config.vm.box = "${var.vm_box}"
      config.vm.synced_folder ".", "/vagrant", disabled: ${var.disable_synced_folder ? "true" : "false"}

    ${local.vagrant_nodes}
    end
  EOT
}

resource "local_file" "vagrantfile" {
  filename        = "${path.module}/Vagrantfile"
  content         = local.vagrantfile_content
  file_permission = "0644"
}

resource "null_resource" "vagrant_up" {
  triggers = {
    vagrantfile_sha = sha256(local_file.vagrantfile.content)
  }

  provisioner "local-exec" {
    working_dir = path.module
    command     = "vagrant up"
  }

  provisioner "local-exec" {
    when        = destroy
    working_dir = path.module
    command     = "vagrant destroy -f"
  }
}
