provider "aws" {
  region  = "eu-central-1"
  profile = "gabro"
}

data "aws_ami" "ubuntu" {
  most_recent = true

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-trusty-14.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }

  owners = ["099720109477"]
}

resource "aws_instance" "finanzio" {
  ami             = "${data.aws_ami.ubuntu.id}"
  instance_type   = "t2.micro"
  key_name        = "${aws_key_pair.ssh.key_name}"
  security_groups = ["${aws_security_group.finanzio.name}"]

  provisioner "remote-exec" {
    inline = [
      "echo \"##### Performing apt-get update #####\"",
      "sudo apt-get update",
      "echo \"##### Installing packages #####\"",
      "sudo apt-get install apt-transport-https ca-certificates curl software-properties-common",
      "curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -",
      "echo \"##### The Docker repo Key fingerprint should be: #####\"",
      "echo 9DC8 5822 9FC7 DD38 854A E2D8 8D81 803C 0EBF CD88",
      "sudo apt-key fingerprint 0EBFCD88",
      "sudo add-apt-repository \"deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable\"",
      "echo \"##### Performing apt-get update #####\"",
      "sudo apt-get update",
      "sudo apt-get install -y docker-ce",
      "sudo usermod -aG docker ubuntu",
      "echo \"##### Installing Docker Compose#####\"",
      "sudo curl -L \"https://github.com/docker/compose/releases/download/1.23.2/docker-compose-$(uname -s)-$(uname -m)\" -o /usr/local/bin/docker-compose",
      "sudo chmod +x /usr/local/bin/docker-compose",
    ]
  }

  connection {
    type = "ssh"
    user = "ubuntu"
  }

  provisioner "file" {
    source      = "docker-compose.yml"
    destination = "docker-compose.yml"
  }

  provisioner "remote-exec" {
    inline = [
      "docker-compose up -d",
    ]
  }
}

resource "aws_security_group" "finanzio" {
  name        = "finanzio"
  description = "allow all output, allow ssh inbound"

  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_key_pair" "ssh" {
  key_name   = "gabro-ssh-key"
  public_key = "${file("~/.ssh/id_rsa.pub")}"
}

data "aws_route53_zone" "primary" {
  name = "gabro.me"
}

resource "aws_route53_record" "finanzio" {
  zone_id = "${data.aws_route53_zone.primary.zone_id}"
  name    = "finanzio.gabro.me"
  type    = "A"
  ttl     = "300"
  records = ["${aws_instance.finanzio.public_ip}"]
}

output "ec2_instance_ip" {
  value = "${aws_instance.finanzio.public_ip}"
}
