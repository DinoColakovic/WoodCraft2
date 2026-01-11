package unze.ptf.woodcraft.woodcraft.service;

import unze.ptf.woodcraft.woodcraft.dao.MaterialDao;
import unze.ptf.woodcraft.woodcraft.dao.ShapeDao;
import unze.ptf.woodcraft.woodcraft.model.Material;
import unze.ptf.woodcraft.woodcraft.model.MaterialType;
import unze.ptf.woodcraft.woodcraft.model.ShapePolygon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EstimationService {
    private final MaterialDao materialDao;
    private final ShapeDao shapeDao;
    private final GeometryService geometryService;

    public EstimationService(MaterialDao materialDao, ShapeDao shapeDao, GeometryService geometryService) {
        this.materialDao = materialDao;
        this.shapeDao = shapeDao;
        this.geometryService = geometryService;
    }

    public List<EstimationSummary> estimate(int documentId, double wastePercent) {
        List<ShapePolygon> shapes = shapeDao.findByDocument(documentId);
        Map<Integer, List<ShapePolygon>> grouped = new HashMap<>();
        for (ShapePolygon shape : shapes) {
            if (shape.getMaterialId() != null) {
                grouped.computeIfAbsent(shape.getMaterialId(), key -> new ArrayList<>()).add(shape);
            }
        }
        List<EstimationSummary> summaries = new ArrayList<>();
        for (Map.Entry<Integer, List<ShapePolygon>> entry : grouped.entrySet()) {
            Optional<Material> materialOptional = materialDao.findById(entry.getKey());
            if (materialOptional.isEmpty()) {
                continue;
            }
            Material material = materialOptional.get();
            summaries.add(buildSummary(material, entry.getValue(), wastePercent));
        }
        return summaries;
    }

    private EstimationSummary buildSummary(Material material, List<ShapePolygon> shapes, double wastePercent) {
        double totalAreaCm2 = 0;
        double totalPerimeterCm = 0;
        for (ShapePolygon shape : shapes) {
            totalAreaCm2 += shape.getAreaCm2() * shape.getQuantity();
            totalPerimeterCm += shape.getPerimeterCm() * shape.getQuantity();
        }

        if (material.getType() == MaterialType.SHEET) {
            double areaM2 = totalAreaCm2 / 10000.0;
            double wasteMultiplier = 1 + (wastePercent / 100.0);
            double adjustedAreaM2 = areaM2 * wasteMultiplier;
            if (material.getSheetPrice() > 0 && material.getSheetAreaCm2() > 0) {
                double sheetAreaM2 = material.getSheetAreaCm2() / 10000.0;
                int sheets = (int) Math.ceil(adjustedAreaM2 / sheetAreaM2);
                double cost = sheets * material.getSheetPrice();
                String details = String.format("Sheets: %d (%.2f m² + %.1f%% waste)", sheets, areaM2, wastePercent);
                return new EstimationSummary(material.getName(), details, cost);
            }
            double cost = adjustedAreaM2 * material.getPricePerSquareMeter();
            String details = String.format("Area: %.2f m² (%.1f%% waste)", adjustedAreaM2, wastePercent);
            return new EstimationSummary(material.getName(), details, cost);
        }

        double totalMeters = totalPerimeterCm / 100.0;
        double cost = totalMeters * material.getPricePerLinearMeter();
        String details = String.format("Linear: %.2f m", totalMeters);
        return new EstimationSummary(material.getName(), details, cost);
    }
}
