# Le mariage de Jib et Cloud Run

Le point de non retour a été atteint et on ne peut plus faire marche arrière, les containers font parti intégrantes du paysage de notre métier. 
### Mais que dois-je faire ?! 
- c’est du travail d’ops, f*** off !!! 
- Docker, pourquoi pas, mais je fais tourner ça comment ??? 
- Kubernetes ? Bordel j’ai déjà du mal à le dire 😱, alors comprendre le concept, j’en ai pour des mois 
- Google n'aurait pas fait un truc pour me faciliter la vie ?
 Effectivement, et comme souvent, Google nous facilite la vie et pas seulement avec une barre de recherche.
## JIB
### Késako ???
Jib est un outil créé par Google qui permet de générer des containers sans le Docker Deamon sur votre machine ni Dockerfile dans votre projet, mais ce n’est pas tout. Généralement, une application Java est défini par un seul layer de type jar qui regroupe l’ensemble du code et des dépendances. Jib propose de découper ce jar en plusieurs layers (classes et dépendences) et permettre un build d’image plus rapide et plus optimisé afin de ne recréer que les layers qui ont été modifiés. Ces layers sont ensuite ajoutés à une base image Java "distroless", qui ne contient exclusivement que les dépendances nécessaires au fonctionnement d’une JVM.
Pour plus d’information => [Distroless](https://github.com/GoogleContainerTools/distroless)
### KomanFège ??
Après toutes ces belles promesses, comment fait-on pour utiliser tout ça ?  Il existe des plugins pour Maven et pour Gradle. Et si vous êtes un peu maso (#sbt), vous pouvez l’implémenter vous même avec la lib jib-core. Nous verrons ici le fontionnement avec le plugin Maven.

## Let's code

On se place ici dans le contexte d'une application Spring Boot générée depuis ‎Spring Initializr.

Commençons par la configuration Maven pour le plugin Jib

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>2.1.0</version>
    <configuration>
        <to>
            <image>charon11/jib-demo</image>
            <tags>${project.version}</tags>
        </to>
    </configuration>
</plugin>
```

et ... ben c'est fini, il n'y a rien d'autre à faire, tout est prêt pour créer notre container.

Dans notre cas, nous allons build le container sans Docker.  
```bash
mvn jib:build
```

Que va t'il se passer ici:
- téléchargement de l'image distroless java sur gcr.io
- création du container à partir de l'image distroless
- upload du container sur la registry saisie dans la configuration (ici Docker Hub). Il faut donc d'abord être connecté sur Docker Hub à l'aide la commande "docker login". Il est possible de préciser les credentials dans la configuration maven.
- done

Il est également possible de builder son container avec Docker, à l'aide de la commande 
```bash
mvn jib:dockerBuild
```
Votre container est du coup upload dans la registry locale.
 
 Oui mais maintenant, il est temps de faire tourner tout ça.

 ## Cloud Run

 ### Re késako ?
 Cloud Run est une plate-forme de service totalement managé de Google permétant de déployer et d'exécuter des containers stateless. Le but de cette plateforme est de profiter des avantages des containers mais sans les potentielles difficultés du management d'une plateforme d'orchestration de container. Autre avantage, la facturation ne se fait que lorsque le container est en activité. On peut assimiler Cloud Run à une Cloud Function mais avec un container. 

### Let's use it

Si on prend en compte que vous avez déjà un projet dans la GCP, vous pouvez accédez au service Cloud Run via la barre de recherche de la console Google Cloud Platform

![Find Cloud Run](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/GotoCloudRun.png)

Il faut ensuite activé le service Cloud Run: 

![Activate Cloud Run](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudRunActivateService.png)


![Cloud Run Service](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudRunService.png)

Vous allez ensuite devoir activer Api Registry:

![Cloud Run Registry](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudRunActivateRegistry.png)


Et enfin pour terminer, il faut installer le Google Cloud SDK en suivant la documentation suivante [Google Cloud SDK](https://cloud.google.com/sdk/docs), puis configurer [gcloud comme Docker credential helper](https://cloud.google.com/container-registry/docs/advanced-authentication#gcloud-helper)


Une fois que tout est installé et configuré, on est up and ready pour utiliser Cloud Run

Nous allons commencer par faire une petite modification dans la configuration de Jib :

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>2.1.0</version>
    <configuration>
        <to>
            <image>gcr.io/cloud-run-jib/jib-demo</image>
            <credHelper>gcloud</credHelper>
            <tags>${project.version}</tags>
        </to>
    </configuration>
</plugin>
```

Maintenant si vous relancez la commande
```bash
mvn jib:build
...
[INFO] Built and pushed image as gcr.io/cloud-run-jib/jib-demo, gcr.io/cloud-run-jib/jib-demo:0.0.1-SNAPSHOT
...
```
vous devriez build et push votre image sur la registry GCloud.

![Cloud Run Registry Container](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudRunRegistryContainer.png)

![Cloud Run Registry Container Version](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudRunRegistryContainerVersion.png)


Une fois que notre container est dans le registry, il est temps de le faire tourner dans Cloud Run, pour cela retournez dans le service Cloud Run et cliquez sur Créer un service
![Cloud Run Service](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudRunService.png) 


Choisissez votre région, le nom de votre service et autoriser les appels non authentifiés:
![Cloud Run Create Service](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudRunCreateService.png) 


Selectionnez ensuite votre container précédemment uploadé et cliquez sur Créer
![Cloud Run Create Service Container](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudRunCreateServiceContainer.png) 

Une fois le service créé, vous avez une url disponible pour y accéder 
![Cloud Run Create Service Created](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudRunCreateServiceCreated.png) 

Et voila, vous avez votre container qui tourne sur le Cloud:

![Cloud Run Service Started](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudRunStarted.png) 


## Fusion

![Fusion](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/fusion.png)


Nous venons de voir que Jib et Cloud Run fonctionnent bien independamment, on va maintenant voir qu'ils peuvent également travailer ensemble et automatiquement.


Il va d'abord falloir ajouter application [Google Cloud Build](https://github.com/marketplace/google-cloud-build) à votre compte Github. Ensuite il faut donner accès à cette application à votre repository:

![Github Apps](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/GitHubApp-GoogleCloudBuild.png)

Une fois l'application activée sur votre repos, nous allons ajouter le fichier de configuration que va utiliser Cloud Build dans le dossier .cloudbuild/cloudbuild-jib-maven.yaml
```yaml
substitutions:
  _IMAGE_NAME: jib-demo

steps:

  - # Uses the Cloud Builder Maven image since it is cached.
    name: gcr.io/cloud-builders/mvn
    dir: /root
    entrypoint: bash
    args:
      - -c
      - # Links the Docker config to /root/.docker/config.json so Jib picks it up.
        # Note that this is only a temporary workaround.
        # See https://github.com/GoogleContainerTools/jib/pull/1479.
        |
        mkdir .docker &&
        ln -s $$HOME/.docker/config.json .docker/config.json

    volumes:
      - name: user.home
        path: /root

  - # Uses the Cloud Builder Maven image.
    name: gcr.io/cloud-builders/mvn
    args:
      # Compiles the application.
      - compile

      # Runs the Jib build by using the latest version of the plugin.
      # To use a specific version, configure the plugin in the pom.xml.
      - com.google.cloud.tools:jib-maven-plugin:build
    volumes:
      - name: user.home
        path: /root

  - # Uses the Cloud Builder GCloud image.
    name: gcr.io/cloud-builders/gcloud
    args:
      # Run the GCloud command to deploy the latest version of the project container
      - beta
      - run
      - deploy
      - ${_IMAGE_NAME}
      - --image
      - gcr.io/${PROJECT_ID}/${_IMAGE_NAME}
      - --region
      - europe-west1
      - --platform
      - managed
      - --allow-unauthenticated
```

Et petite subtilité, on va commenter "image" et "credHelper" dans la config Maven car les identifiants sont configurés dans la première étape du build et l'image est configurée dans la seconde étape

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>2.1.0</version>
    <configuration>
        <to>
            <!--<image>gcr.io/cloud-run-jib/jib-demo</image>
            <credHelper>gcloud</credHelper>-->
            <tags>${project.version}</tags>
        </to>
    </configuration>
</plugin>
```

On retourne ensuite dans la console de la GCP pour activer Cloud Build

![Cloud Build API](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudBuildApi.png)

![Cloud Build](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudBuild.png)

On va ensuite connecter notre repos à Cloud Build

![Cloud Build Connect Step 1](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudBuildConnect-1.png)

![Cloud Build Connect Step 2](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudBuildConnect-2.png)

Et on skip la création d'un déclencheur, que l'on va faire juste après

![Cloud Build Connected](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudBuildConnected.png)

Maintenant nous allons créer un déclencheur

![Cloud Build Trigger](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudBuildTrigger.png)

![Cloud Build Trigger Added](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudBuildTriggerAdded.png)

Une fois ajouté, on peut tester notre déclencheur en cliquant sur "Exécuter le déclencheur"

![Cloud Build Trigger Success](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudBuildSuccess.png)

![Cloud Build Trigger History](https://raw.githubusercontent.com/Charon11/jib-cloud-run/master/resources/CloudBuildHist.png)

Il ne reste plus qu'à tester votre déclencheur en faisant un push sur votre repos github, et vérifier que le build a réussi.