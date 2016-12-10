#!/bin/sh

base_dir=$(dirname $0)

for pkg in $@
do
    pkg_dir="${base_dir}/${pkg}"
    cat <<MAKE
${base_dir}/lib/$(basename ${pkg}).stamp: \$(call lookup_scala_srcs, ${pkg_dir}) \$(rocketchip_stamp)
	mkdir -p ${base_dir}/lib
	rm -f ${pkg_dir}/lib
	ln -s ${base_dir}/lib ${pkg_dir}/lib
	cd ${pkg_dir} && \$(SBT) publish-local
	touch \$@
MAKE
done
