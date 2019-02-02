package finanzio.config

case class FlywayConfig(
    clean: Boolean,
    validate: Boolean,
    migrate: Boolean,
    locations: String
)
