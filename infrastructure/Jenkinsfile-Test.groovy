pipeline {
  agent any

  environment {
    AWS_ACCESS_KEY_ID = credentials('AWS_ACCESS_KEY_ID')
    AWS_SECRET_ACCESS_KEY = credentials('AWS_SECRET_ACCESS_KEY')
    DOCKER_PROJECT_NAME = "matomo"
    SERVER_PORT = "1042"
    
  }

  stages {
    stage('Configure') {
      steps {
        sh """
          rm -f .env
          echo 'COMPOSE_PROJECT_NAME=${DOCKER_PROJECT_NAME}' >> .env
          echo 'SERVER_PORT=${SERVER_PORT}' >> .env
          echo 'MYSQL_PASSWORD=' >> .env
          echo 'MYSQL_DATABASE=matomo' >> .env
          echo 'MYSQL_USER=matomo' >> .env
          echo 'MATOMO_DATABASE_ADAPTER=mysql' >> .env
          echo 'MATOMO_DATABASE_TABLES_PREFIX=matomo_' >> .env
          echo 'MATOMO_DATABASE_USERNAME=matomo' >> .env
          echo 'MATOMO_DATABASE_PASSWORD=' >> .env
          echo 'MATOMO_DATABASE_DBNAME=matomo' >> .env
          rm -f matomo.conf
          echo 'upstream php-handler {' >> matomo.conf
          echo '  server app:9000;' >> matomo.conf
          echo '}' >> matomo.conf
          echo '  server {' >> matomo.conf
          echo '    listen 80;' >> matomo.conf
          echo '    add_header Referrer-Policy origin; # make sure outgoing links don't show the URL to the Matomo instance' >> matomo.conf
          echo '    root /var/www/html; # replace with path to your matomo instance' >> matomo.conf
          echo '    index index.php;' >> matomo.conf
          echo '    try_files $uri $uri/ =404;' >> matomo.conf
          echo '    ## only allow accessing the following php files' >> matomo.conf
          echo '    location ~ ^/(index|matomo|piwik|js/index|plugins/HeatmapSessionRecording/configs).php {' >> matomo.conf
          echo '      # regex to split $uri to $fastcgi_script_name and $fastcgi_path' >> matomo.conf
          echo '      fastcgi_split_path_info ^(.+\\.php)(/.+)$;' >> matomo.conf
          echo '      # Check that the PHP script exists before passing it' >> matomo.conf
          echo '      try_files $fastcgi_script_name =404;' >> matomo.conf
          echo '      include fastcgi_params;' >> matomo.conf
          echo '      fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;' >> matomo.conf
          echo '      fastcgi_param PATH_INFO $fastcgi_path_info;' >> matomo.conf
          echo '      fastcgi_param HTTP_PROXY ""; # prohibit httpoxy: https://httpoxy.org/' >> matomo.conf
          echo '      fastcgi_pass php-handler;' >> matomo.conf
          echo '    }' >> matomo.conf
          echo '    ## deny access to all other .php files' >> matomo.conf
          echo '    location ~* ^.+\.php$ {' >> matomo.conf
          echo '      deny all;' >> matomo.conf
          echo '      return 403;' >> matomo.conf
          echo '    }' >> matomo.conf
          echo '    ## disable all access to the following directories' >> matomo.conf
          echo '    location ~ /(config|tmp|core|lang) {' >> matomo.conf
          echo '      deny all;' >> matomo.conf
          echo '      return 403; # replace with 404 to not show these directories exist' >> matomo.conf
          echo '    }' >> matomo.conf
          echo '    location ~ /\\.ht {' >> matomo.conf
          echo '      deny all;' >> matomo.conf
          echo '      return 403;' >> matomo.conf
          echo '    }' >> matomo.conf
          echo '    location ~ js/container_.*_preview\\.js$ {' >> matomo.conf
          echo '      expires off;' >> matomo.conf
          echo '      add_header Cache-Control 'private, no-cache, no-store';' >> matomo.conf
          echo '    }' >> matomo.conf
          echo '    location ~ \\.(gif|ico|jpg|png|svg|js|css|htm|html|mp3|mp4|wav|ogg|avi|ttf|eot|woff|woff2|json)$ {' >> matomo.conf
          echo '      allow all;' >> matomo.conf
          echo '      ## Cache images,CSS,JS and webfonts for an hour' >> matomo.conf
          echo '      ## Increasing the duration may improve the load-time, but may cause old files to show after an Matomo upgrade' >> matomo.conf
          echo '      expires 1h;' >> matomo.conf
          echo '      add_header Pragma public;' >> matomo.conf
          echo '      add_header Cache-Control "public";' >> matomo.conf
          echo '    }' >> matomo.conf
          echo '    location ~ /(libs|vendor|plugins|misc/user) {' >> matomo.conf
          echo '      deny all;' >> matomo.conf
          echo '      return 403;' >> matomo.conf
          echo '    }' >> matomo.conf
          echo '   ## properly display textfiles in root directory' >> matomo.conf
          echo '   location ~/(.*\\.md|LEGALNOTICE|LICENSE) {' >> matomo.conf
          echo '      default_type text/plain;' >> matomo.conf
          echo '    }' >> matomo.conf
          echo '  }' >> matomo.conf' >> matomo.conf
        """
      }
    }
    stage('Deploy') {
      steps {
        sshagent(['jenkins-ssh-key']) {
          sh """
            (cd infrastructure/ansible && ansible-galaxy install -f -r requirements.yml)
            (cd infrastructure/ansible && ansible-playbook --limit=test deploy.yml --extra-vars "release_name=${BUILD_NUMBER}")
          """
        }
      }
    }
  }
}
