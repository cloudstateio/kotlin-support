# Make Cloudstate Kotlin documentation

module   := kotlin
upstream := cloudstateio/kotlin-support
branch   := docs/current
sources  := src build/src/managed

cloudstate_antora_download := https://github.com/cloudstateio/cloudstate-antora/raw/master/cloudstate-antora

cloudstate_antora := .cache/bin/cloudstate-antora
descriptor := build/site.yml

.SILENT:

build: clean managed validate html

${cloudstate_antora}:
	mkdir -p $$(dirname ${cloudstate_antora})
	curl -Lo ${cloudstate_antora} ${cloudstate_antora_download}
	chmod +x ${cloudstate_antora}

clean-cache:
	rm -rf .cache

update: clean-cache ${cloudstate_antora}

clean: ${cloudstate_antora}
	${cloudstate_antora} clean

managed: attributes
	mkdir -p build/src/managed
	cp src/antora.yml build/src/managed/antora.yml

attributes: ${cloudstate_antora}
	mkdir -p build/src/managed/modules/kotlin/partials
	${cloudstate_antora} version --prefix '' | xargs -0  printf ":cloudstate-kotlin-support-version: %s" \
		> build/src/managed/modules/kotlin/partials/attributes.adoc

${descriptor}: ${cloudstate_antora}
	mkdir -p $$(dirname ${descriptor})
	${cloudstate_antora} source --preview --upstream ${upstream} ${sources} > build/source.yml
	${cloudstate_antora} site --preview --exclude ${module} build/source.yml > ${descriptor}

validate: ${descriptor}
	${cloudstate_antora} validate ${descriptor}

html: ${descriptor}
	${cloudstate_antora} build ${descriptor}

validate-links: ${cloudstate_antora}
	${cloudstate_antora} validate --no-xrefs --links --links-config config/validate-links.json

deploy: clean managed
	${cloudstate_antora} deploy --module ${module} --upstream ${upstream} --branch ${branch} ${sources}
