FROM hseeberger/scala-sbt:8u181_2.12.8_1.2.8 as build
WORKDIR /workdir
ADD project project
ADD build.sbt .
RUN sbt finanzio/update
ADD finanzio finanzio
ADD saltedge saltedge
ADD splitwise splitwise
ADD oauth oauth
RUN sbt finanzio/assembly

FROM openjdk:8-jre-alpine
COPY --from=build /workdir/finanzio/target/scala-2.12/finanzio.jar /srv/api.jar
EXPOSE 8080
CMD /usr/bin/java -cp /srv/api.jar finanzio.Main


