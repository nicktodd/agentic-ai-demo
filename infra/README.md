# Infrastructure (AWS sandbox)

The [OpenTofu](https://opentofu.org) configuration in `terraform/` creates everything the app
needs to run on AWS (the directory keeps the conventional name; the `.tf` files are plain HCL
and work unchanged with HashiCorp Terraform >= 1.5 if that is what you have):

```
Internet -> ALB (:80) -> ECS Fargate task (:8080, Spring Boot) <- image from ECR
                                   |
                              CloudWatch Logs (/ecs/portfolio-valuation)
```

It deliberately uses the **default VPC** and **plain HTTP** to stay cheap and readable.
Comments in each `.tf` file say what a production set-up would do differently.

## Prerequisites

* [OpenTofu](https://opentofu.org/docs/intro/install/) >= 1.6 (`brew install opentofu`), or
  [Terraform](https://developer.hashicorp.com/terraform/install) >= 1.5. The scripts use `tofu`
  if it is on the PATH, else `terraform`; override with `TF_BIN=...`.
* [AWS CLI v2](https://docs.aws.amazon.com/cli/) configured for the **sandbox** account
  (`aws sts get-caller-identity` should show the sandbox account id)
* Docker (to build and push the first image)

## First deploy (the chicken-and-egg problem)

OpenTofu creates the ECR repository *and* an ECS service that wants to run an image from it.
On the very first apply there is no image yet, so the service would sit there failing to start
until one appears. Two ways round it; we recommend the first:

**Option A — push an image from your laptop, then apply (recommended)**

First make sure the CLI is talking to the right account:

```bash
aws sts get-caller-identity --query Account --output text   # must print the sandbox account id
```

Then, from the repo root:

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars   # edit: set owner to your email
tofu init
tofu apply -target=aws_ecr_repository.app  # create just the ECR repo (type "yes" at the prompt)

# Build + push the first image, then create everything else (about 20 resources, ~2 min):
cd ../..
./scripts/deploy_manual.sh --apply              # type "yes" at the apply prompt

# The ALB target takes another 2-3 minutes to turn healthy:
cd infra/terraform
curl -s "$(tofu output -raw health_url)"        # {"status":"UP"}
```

Without `--apply`, `deploy_manual.sh` only builds and pushes; you then run `tofu apply` and
`tofu output health_url` yourself from `infra/terraform`.

Or by hand, if you want to see each command:

```bash
AWS_REGION=eu-west-2
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR=$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/portfolio-valuation

docker build -t portfolio-valuation:latest .
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR
docker tag portfolio-valuation:latest $ECR:latest
docker push $ECR:latest
```

**Option B — apply with `desired_count = 0`, let Jenkins do the first deploy**

```bash
tofu apply -var desired_count=0
# ... Jenkins pushes and registers a task definition on the next main build ...
aws ecs update-service --cluster portfolio-valuation-cluster --service portfolio-valuation-service --desired-count 1
```

(`desired_count` is in `ignore_changes`, so OpenTofu won't scale it back to 0 later.)

## Where the state lives (if you are used to CloudFormation / CDK)

CloudFormation keeps the stack state inside AWS. OpenTofu keeps it in a file,
`infra/terraform/terraform.tfstate`: JSON mapping every resource in the `.tf` files to the real
AWS id. `tofu plan` diffs the files against that file (and against AWS), and `tofu destroy`
deletes whatever is listed in it. Consequences:

* The file is git-ignored, so it exists only on the laptop that ran `apply`. **Run `apply` and
  `destroy` from the same machine and directory.** If you lose it OpenTofu forgets the stack
  exists and you must delete the resources by hand in the console (or `tofu import` them).
* It holds ARNs, names and the ALB DNS name, no secrets, so it is not sensitive, just precious.
* A real project points the `backend` block in `versions.tf` at an S3 bucket with state locking so
  a team can share it. For a sandbox that lives a few days at a time, local state is fine, and
  the difference makes a good talking point with the audience.

## After the first apply

Jenkins (`Jenkinsfile`, branch `main`) pushes a new image per commit, registers a new task
definition revision and updates the service. OpenTofu ignores those two fields, so you can
re-run `tofu apply` for infra changes without rolling the app back.

Useful commands:

```bash
tofu output                                     # URLs, names, ARNs
aws logs tail /ecs/portfolio-valuation --follow # app logs
aws ecs describe-services --cluster portfolio-valuation-cluster --services portfolio-valuation-service \
  --query 'services[0].deployments'
```

## Cost (rough, eu-west-2)

| Item                              | ~USD/day |
|-----------------------------------|----------|
| Fargate 0.5 vCPU / 1 GB, 1 task   | 0.60     |
| Application Load Balancer         | 0.65 + tiny LCU charge |
| ECR storage, CloudWatch logs      | cents    |
| **Total**                         | **~ $1.50 / day** |

Cheap, but not free: **run `scripts/teardown.sh` after the session.**

## IAM for Jenkins

Jenkins runs on an EC2 instance in the sandbox account and uses an **instance profile role**,
so no access keys exist anywhere. Create an IAM role for EC2 (e.g. `jenkins-demo-role`), attach
the policy in [`jenkins-iam-policy.json`](jenkins-iam-policy.json) as an inline policy (replace
`ACCOUNT_ID`), and attach the role to the instance. Verify from the instance with
`sudo -u jenkins aws sts get-caller-identity`. The policy grants only what the pipeline uses:

* `sts:GetCallerIdentity` — the pipeline uses it to find the account id for the ECR hostname
* `ecr:GetAuthorizationToken` + push/pull on the one repository
* `ecs:Describe*`, `ecs:RegisterTaskDefinition`, `ecs:UpdateService`
* `iam:PassRole` on the task execution role only (needed to register a task definition that uses it)
* `elasticloadbalancing:DescribeLoadBalancers` — to print the URL

Running OpenTofu itself needs far broader rights (creating VPC security groups, IAM roles,
ALBs ...), so do that from your own admin-ish sandbox credentials on the laptop, not from Jenkins.

## Tear down

```bash
./scripts/teardown.sh        # tofu destroy + log group clean-up
```
