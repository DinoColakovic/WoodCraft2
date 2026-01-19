package unze.ptf.woodcraft.woodcraft.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.geometry.Point2D;
import unze.ptf.woodcraft.woodcraft.dao.*;
import unze.ptf.woodcraft.woodcraft.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ProjectPackageService {

    private final DocumentDao documentDao;
    private final NodeDao nodeDao;
    private final EdgeDao edgeDao;
    private final GuideDao guideDao;
    private final ShapeDao shapeDao;
    private final ManualShapeDao manualShapeDao;
    private final DimensionDao dimensionDao;
    private final MaterialDao materialDao;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ProjectPackageService(
            DocumentDao documentDao,
            NodeDao nodeDao,
            EdgeDao edgeDao,
            GuideDao guideDao,
            ShapeDao shapeDao,
            ManualShapeDao manualShapeDao,
            DimensionDao dimensionDao,
            MaterialDao materialDao
    ) {
        this.documentDao = documentDao;
        this.nodeDao = nodeDao;
        this.edgeDao = edgeDao;
        this.guideDao = guideDao;
        this.shapeDao = shapeDao;
        this.manualShapeDao = manualShapeDao;
        this.dimensionDao = dimensionDao;
        this.materialDao = materialDao;
    }

    // =========================
    // EXPORT
    // =========================
    public void exportDocument(int documentId, int userId, Path zipOut) throws IOException {
        Document doc = documentDao.findById(documentId, userId)
                .orElseThrow(() -> new IllegalStateException("Dokument ne postoji ili nije od trenutnog korisnika."));

        List<NodePoint> nodes = nodeDao.findByDocument(documentId);
        List<Edge> edges = edgeDao.findByDocument(documentId);
        List<Guide> guides = guideDao.findByDocument(documentId);
        List<ShapePolygon> shapes = shapeDao.findByDocument(documentId);
        List<ManualShape> manualShapes = manualShapeDao.findByDocument(documentId);
        List<Dimension> dimensions = dimensionDao.findByDocument(documentId);

        // materials used in this document (from shapes.material_id)
        Set<Integer> usedMaterialIds = new HashSet<Integer>();
        for (ShapePolygon s : shapes) {
            if (s.getMaterialId() != null) usedMaterialIds.add(s.getMaterialId());
        }

        List<Material> usedMaterials = new ArrayList<Material>();
        for (Integer mid : usedMaterialIds) {
            materialDao.findById(mid).ifPresent(usedMaterials::add);
        }

        ProjectPackage pack = new ProjectPackage();
        pack.schemaVersion = 1;

        pack.document = new ProjectPackage.DocumentDto();
        pack.document.name = doc.getName();
        pack.document.widthCm = doc.getWidthCm();
        pack.document.heightCm = doc.getHeightCm();
        pack.document.kerfMm = doc.getKerfMm();
        pack.document.unitSystem = doc.getUnitSystem().name();

        pack.nodes = new ArrayList<ProjectPackage.NodeDto>();
        for (NodePoint n : nodes) {
            ProjectPackage.NodeDto dto = new ProjectPackage.NodeDto();
            dto.id = n.getId();
            dto.xCm = n.getXCm();
            dto.yCm = n.getYCm();
            pack.nodes.add(dto);
        }

        pack.edges = new ArrayList<ProjectPackage.EdgeDto>();
        for (Edge e : edges) {
            ProjectPackage.EdgeDto dto = new ProjectPackage.EdgeDto();
            dto.id = e.getId();
            dto.startNodeId = e.getStartNodeId();
            dto.endNodeId = e.getEndNodeId();
            dto.controlStartXCm = e.getControlStartXCm();
            dto.controlStartYCm = e.getControlStartYCm();
            dto.controlEndXCm = e.getControlEndXCm();
            dto.controlEndYCm = e.getControlEndYCm();
            pack.edges.add(dto);
        }

        pack.guides = new ArrayList<ProjectPackage.GuideDto>();
        for (Guide g : guides) {
            ProjectPackage.GuideDto dto = new ProjectPackage.GuideDto();
            dto.id = g.getId();
            dto.orientation = g.getOrientation().name();
            dto.positionCm = g.getPositionCm();
            pack.guides.add(dto);
        }

        pack.shapes = new ArrayList<ProjectPackage.ShapeDto>();
        for (ShapePolygon s : shapes) {
            ProjectPackage.ShapeDto dto = new ProjectPackage.ShapeDto();
            dto.id = s.getId();
            dto.materialId = s.getMaterialId();
            dto.quantity = s.getQuantity();
            dto.nodeIds = joinIds(s.getNodeIds());
            dto.areaCm2 = s.getAreaCm2();
            dto.perimeterCm = s.getPerimeterCm();
            pack.shapes.add(dto);
        }

        pack.manualShapes = new ArrayList<ProjectPackage.ManualShapeDto>();
        for (ManualShape ms : manualShapes) {
            ProjectPackage.ManualShapeDto dto = new ProjectPackage.ManualShapeDto();
            dto.id = ms.getId();
            dto.points = serializePoints(ms.getPoints()); // identicno DAO formatu
            pack.manualShapes.add(dto);
        }

        pack.dimensions = new ArrayList<ProjectPackage.DimensionDto>();
        for (Dimension d : dimensions) {
            ProjectPackage.DimensionDto dto = new ProjectPackage.DimensionDto();
            dto.id = d.getId();
            dto.type = d.getType().name();

            dto.startXCm = d.getStartXCm();
            dto.startYCm = d.getStartYCm();
            dto.endXCm = d.getEndXCm();
            dto.endYCm = d.getEndYCm();

            dto.offsetXCm = d.getOffsetXCm();
            dto.offsetYCm = d.getOffsetYCm();

            dto.startNodeId = d.getStartNodeId();
            dto.endNodeId = d.getEndNodeId();
            pack.dimensions.add(dto);
        }

        pack.materials = new ArrayList<ProjectPackage.MaterialDto>();

        // write zip: assets + project.json
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipOut))) {

            for (Material m : usedMaterials) {
                ProjectPackage.MaterialDto dto = new ProjectPackage.MaterialDto();
                dto.id = m.getId();
                dto.name = m.getName();
                dto.type = m.getType().name();
                dto.sheetWidthCm = m.getSheetWidthCm();
                dto.sheetHeightCm = m.getSheetHeightCm();
                dto.sheetPrice = m.getSheetPrice();
                dto.pricePerSquareMeter = m.getPricePerSquareMeter();
                dto.pricePerLinearMeter = m.getPricePerLinearMeter();
                dto.grainDirection = m.getGrainDirection().name();
                dto.edgeBandingCostPerMeter = m.getEdgeBandingCostPerMeter();

                // copy image into /assets inside zip (if exists)
                if (m.getImagePath() != null && !m.getImagePath().isBlank()) {
                    File img = new File(m.getImagePath());
                    if (img.exists() && img.isFile()) {
                        String ext = getExt(img.getName());
                        String entryName = "assets/material_" + m.getId() + ext;
                        putFile(zos, entryName, img.toPath());
                        dto.imagePath = entryName; // relative inside zip
                    }
                }

                // ✅ OVO JE BITNO: dodaj u JSON listu
                pack.materials.add(dto);
            }

            // project.json
            zos.putNextEntry(new ZipEntry("project.json"));
            zos.write(gson.toJson(pack).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    // =========================
    // IMPORT
    // =========================
    public int importPackage(Path zipPath, int userId, String finalName) throws IOException {
        Map<String, byte[]> zipEntries = readAllZipEntries(zipPath);

        if (!zipEntries.containsKey("project.json")) {
            throw new IllegalStateException("Paket nema project.json");
        }

        ProjectPackage pack = gson.fromJson(
                new String(zipEntries.get("project.json"), StandardCharsets.UTF_8),
                ProjectPackage.class
        );

        if (pack == null || pack.schemaVersion != 1) {
            throw new IllegalStateException("Nepodrzan schemaVersion");
        }
        if (pack.document == null) {
            throw new IllegalStateException("project.json nema document");
        }

        // 1) create new document
        int newDocId = documentDao.createDocument(
                userId,
                finalName,
                pack.document.widthCm,
                pack.document.heightCm,
                pack.document.kerfMm,
                UnitSystem.valueOf(pack.document.unitSystem)
        );

        // 2) materials (old -> new) + extract images to local storage
        Map<Integer, Integer> materialMap = new HashMap<Integer, Integer>();
        Path assetsDir = ensureAssetsDir(userId);

        if (pack.materials != null) {
            for (ProjectPackage.MaterialDto m : pack.materials) {
                String newImagePath = null;

                if (m.imagePath != null && zipEntries.containsKey(m.imagePath)) {
                    String fileName = Paths.get(m.imagePath).getFileName().toString();
                    Path out = assetsDir.resolve(fileName);
                    Files.write(out, zipEntries.get(m.imagePath));
                    newImagePath = out.toAbsolutePath().toString();
                }

                Material material = new Material(
                        -1,
                        userId,
                        m.name,
                        MaterialType.valueOf(m.type),
                        m.sheetWidthCm,
                        m.sheetHeightCm,
                        m.sheetPrice,
                        m.pricePerSquareMeter,
                        m.pricePerLinearMeter,
                        newImagePath,
                        GrainDirection.valueOf(m.grainDirection),
                        m.edgeBandingCostPerMeter
                );

                int newMatId = materialDao.create(material);
                materialMap.put(m.id, newMatId);
            }
        }

        // 3) nodes (old -> new)
        Map<Integer, Integer> nodeMap = new HashMap<Integer, Integer>();
        if (pack.nodes != null) {
            for (ProjectPackage.NodeDto n : pack.nodes) {
                NodePoint created = nodeDao.create(newDocId, n.xCm, n.yCm);
                nodeMap.put(n.id, created.getId());
            }
        }

        // 4) edges
        if (pack.edges != null) {
            for (ProjectPackage.EdgeDto e : pack.edges) {
                int newStart = mustMap(nodeMap, e.startNodeId, "edge.startNodeId");
                int newEnd = mustMap(nodeMap, e.endNodeId, "edge.endNodeId");

                Edge created = edgeDao.create(newDocId, newStart, newEnd);

                if (e.controlStartXCm != null || e.controlStartYCm != null || e.controlEndXCm != null || e.controlEndYCm != null) {
                    edgeDao.updateControls(created.getId(), e.controlStartXCm, e.controlStartYCm, e.controlEndXCm, e.controlEndYCm);
                }
            }
        }

        // 5) guides
        if (pack.guides != null) {
            for (ProjectPackage.GuideDto g : pack.guides) {
                guideDao.create(newDocId, Guide.Orientation.valueOf(g.orientation), g.positionCm);
            }
        }

        // 6) manual shapes
        if (pack.manualShapes != null) {
            for (ProjectPackage.ManualShapeDto ms : pack.manualShapes) {
                ManualShape shape = new ManualShape(-1, newDocId, parsePoints(ms.points));
                manualShapeDao.create(shape);
            }
        }

        // 7) dimensions
        if (pack.dimensions != null) {
            for (ProjectPackage.DimensionDto d : pack.dimensions) {
                Integer newStartNode = (d.startNodeId == null) ? null : mustMap(nodeMap, d.startNodeId, "dimension.startNodeId");
                Integer newEndNode = (d.endNodeId == null) ? null : mustMap(nodeMap, d.endNodeId, "dimension.endNodeId");

                dimensionDao.create(
                        newDocId,
                        d.startXCm, d.startYCm,
                        d.endXCm, d.endYCm,
                        d.offsetXCm, d.offsetYCm,
                        DimensionType.valueOf(d.type),
                        newStartNode,
                        newEndNode
                );
            }
        }

        // 8) shapes (remap material + node_ids)
        if (pack.shapes != null) {
            for (ProjectPackage.ShapeDto s : pack.shapes) {

                Integer newMatId = null;
                if (s.materialId != null) {
                    newMatId = materialMap.get(s.materialId);
                    if (newMatId == null) {
                        throw new IllegalStateException(
                                "Shape referencira materialId=" + s.materialId + " ali taj materijal nije u paketu."
                        );
                    }
                }

                List<Integer> newNodeIds = remapNodeIdsStringStrict(s.nodeIds, nodeMap);

                ShapePolygon shape = new ShapePolygon(
                        -1,
                        newDocId,
                        newMatId,
                        s.quantity,
                        newNodeIds,
                        List.of(),
                        s.areaCm2,
                        s.perimeterCm
                );

                shapeDao.createShape(shape);
            }
        }

        return newDocId;
    }

    // For UI: suggests name from package
    public String readSuggestedName(Path zipPath) throws IOException {
        Map<String, byte[]> entries = readAllZipEntries(zipPath);
        if (!entries.containsKey("project.json")) return "Uvezeni projekat";

        ProjectPackage pack = gson.fromJson(
                new String(entries.get("project.json"), StandardCharsets.UTF_8),
                ProjectPackage.class
        );

        if (pack == null || pack.document == null || pack.document.name == null || pack.document.name.isBlank()) {
            return "Uvezeni projekat";
        }
        return pack.document.name;
    }

    // =========================
    // Helpers
    // =========================

    private static Map<String, byte[]> readAllZipEntries(Path zipPath) throws IOException {
        Map<String, byte[]> map = new HashMap<String, byte[]>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry e;
            byte[] buffer = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                int r;
                while ((r = zis.read(buffer)) > 0) {
                    bout.write(buffer, 0, r);
                }
                map.put(e.getName(), bout.toByteArray());
            }
        }
        return map;
    }

    private static void putFile(ZipOutputStream zos, String entryName, Path file) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        Files.copy(file, zos);
        zos.closeEntry();
    }

    private static String getExt(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i) : "";
    }

    private static String joinIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            sb.append(ids.get(i));
            if (i < ids.size() - 1) sb.append(',');
        }
        return sb.toString();
    }

    // ✅ STRICT remap: ne dozvoljava "preskakanje" nodeova
    private static List<Integer> remapNodeIdsStringStrict(String value, Map<Integer, Integer> nodeMap) {
        if (value == null || value.isBlank()) return List.of();

        String[] parts = value.split(",");
        List<Integer> out = new ArrayList<Integer>();

        for (String p : parts) {
            int oldId;
            try {
                oldId = Integer.parseInt(p.trim());
            } catch (NumberFormatException ex) {
                throw new IllegalStateException("Neispravan node id u shapes.nodeIds: " + p);
            }

            Integer newId = nodeMap.get(oldId);
            if (newId == null) {
                throw new IllegalStateException("Shape referencira nodeId=" + oldId + " ali taj node nije u paketu.");
            }
            out.add(newId);
        }

        return out;
    }

    private static Integer mustMap(Map<Integer, Integer> map, int oldId, String label) {
        Integer v = map.get(oldId);
        if (v == null) throw new IllegalStateException("Ne mogu remapovati " + label + ": " + oldId);
        return v;
    }

    // ManualShape format identican ManualShapeDao (x:y,x:y)
    private static String serializePoints(List<Point2D> points) {
        if (points == null || points.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            Point2D p = points.get(i);
            builder.append(p.getX()).append(':').append(p.getY());
            if (i < points.size() - 1) builder.append(',');
        }
        return builder.toString();
    }

    private static List<Point2D> parsePoints(String value) {
        List<Point2D> points = new ArrayList<Point2D>();
        if (value == null || value.isBlank()) return points;

        String[] parts = value.split(",");
        for (String part : parts) {
            String[] coords = part.trim().split(":");
            if (coords.length != 2) continue;
            try {
                double x = Double.parseDouble(coords[0]);
                double y = Double.parseDouble(coords[1]);
                points.add(new Point2D(x, y));
            } catch (NumberFormatException ignored) {}
        }
        return points;
    }

    private static Path ensureAssetsDir(int userId) throws IOException {
        Path dir = Paths.get(System.getProperty("user.home"), ".woodcraft", "assets", String.valueOf(userId));
        Files.createDirectories(dir);
        return dir;
    }
}
