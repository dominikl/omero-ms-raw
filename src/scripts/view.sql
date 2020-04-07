-- CREATE EXTENSION btree_gin;
-- CREATE EXTENSION pg_trgm;

CREATE MATERIALIZED VIEW _mapr_containers (name, value, ns, annotation, image, dataset, project, field, well, plate, screen) AS
  SELECT nv.name, nv.value, a.ns, a.id, ial.parent, dil.parent, pdl.parent, ws.id, w.id, p.id, spl.parent
  FROM annotation_mapvalue nv
    INNER JOIN annotation a ON (a.id = nv.annotation_id)
    INNER JOIN imageannotationlink ial ON (ial.child = a.id)
    LEFT OUTER JOIN datasetimagelink dil ON (dil.child = ial.parent)
    LEFT OUTER JOIN projectdatasetlink pdl ON (pdl.child = dil.parent)
    LEFT OUTER JOIN wellsample ws ON (ws.image = ial.parent)
    LEFT OUTER JOIN well w ON (w.id = ws.well)
    LEFT OUTER JOIN plate p ON (p.id = w.plate)
    LEFT OUTER JOIN screenplatelink spl ON (spl.child = p.id);

CREATE INDEX _mapr_containers_name ON _mapr_containers (name);
CREATE INDEX _mapr_containers_value ON _mapr_containers USING GIN(value gin_trgm_ops);
CREATE INDEX _mapr_containers_value_lower ON _mapr_containers USING GIN(LOWER(value) gin_trgm_ops);
CREATE INDEX _mapr_containers_name_value_lower ON _mapr_containers USING GIN(name, LOWER(value) gin_trgm_ops);

CREATE INDEX _mapr_containers_ns ON _mapr_containers (ns);
CREATE INDEX _mapr_containers_annotation ON _mapr_containers (annotation);
CREATE INDEX _mapr_containers_image ON _mapr_containers (image);
CREATE INDEX _mapr_containers_dataset ON _mapr_containers (dataset);
CREATE INDEX _mapr_containers_project ON _mapr_containers (project);
CREATE INDEX _mapr_containers_field ON _mapr_containers (field);
CREATE INDEX _mapr_containers_well ON _mapr_containers (well);
CREATE INDEX _mapr_containers_plate ON _mapr_containers (plate);
CREATE INDEX _mapr_containers_screen ON _mapr_containers (screen);
