/***********************************************************
 ** Author: Nadia Davidson <nadia.davidson@mcri.edu.au>
 ** Last Update: 28/2/2018
 *********************************************************/

VERSION="1.00"

//option strings to pass to tools
trinity_options="--max_memory 50G --normalize_reads"
hisat2_options=""
stringtie_options=""
stringtie_merge_options=""
blat_options="-minScore=200 -minIdentity=98"
blat_related_options="-t=dnax -q=dnax -minScore=200"
featurecount_gene_options="--primary -p"
featurecount_block_options="--primary -p --fraction -O"

codeBase = file(bpipe.Config.config.script).parentFile.absolutePath
load codeBase+"/tools.groovy"

load codeBase+"/bpipe_stages/genome_guided_assembly.groovy"
load codeBase+"/bpipe_stages/build_genome_superTranscriptome.groovy"
load codeBase+"/bpipe_stages/build_relatives_superTranscriptome.groovy"
load codeBase+"/bpipe_stages/de_novo_assembly.groovy"
load codeBase+"/bpipe_stages/cluster.groovy"
load codeBase+"/bpipe_stages/run_lace.groovy"
load codeBase+"/bpipe_stages/map_reads.groovy"
load codeBase+"/bpipe_stages/get_counts.groovy"
load codeBase+"/bpipe_stages/get_stats.groovy"

load args[0]

/******************* Here are the pipeline stages **********************/

set_input = {
   def files=reads_R1.split(",")+reads_R2.split(",")
   forward files
}

run_check = {
    doc "check that the data files exist"
    produce("checks_passed") {
        exec """
            echo "Running necklace version $VERSION" ;
	    echo "Using ${bpipe.Config.config.maxThreads} threads" ;
            echo "Checking for the data files..." ;
	    for i in $genome $annotation $annotation_related_species $genome_related_species $inputs.gz ; 
                 do ls $i 2>/dev/null || { echo "CAN'T FIND ${i}..." ;
		 echo "PLEASE FIX PATH... STOPPING NOW" ; exit 1  ; } ; 
	    done ;
            echo "All looking good" ;
            echo "running  necklace version $VERSION.. checks passed" > $output
        ""","checks"
    }
}

nthreads=bpipe.Config.config.maxThreads

run { set_input + run_check + //single thread 
    [ build_genome_guided_assembly + build_genome_superTranscriptome, 
    de_novo_assemble.using(threads: nthreads-2), 
    build_relatives_superTranscriptome ] + 
    cluster_files +
    run_lace.using(threads: nthreads) + 
    map_reads.using(threads: nthreads) + 
    get_counts.using(threads: nthreads) + 
    get_stats } //single thread



