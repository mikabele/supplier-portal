# Supplier portal server

 Supplier portal is a server-side application that gives suppliers a possibility to manage their products, so the clients are able to browse and order the products provided by multiple suppliers.


## Stack

- Scala
- Http4s
- ScalaTest
- Cats Core/Effect
- Circe
- Doobie

## Requirements

- Docker
- Docker-compose (version >= 3.9)

## Usage

```shell
docker-compose up -d docker/docker-compose.yml
```

After this command you will see 3 running containers : DB, main server and scheduler for email notification

## API

TODO - add request examples
