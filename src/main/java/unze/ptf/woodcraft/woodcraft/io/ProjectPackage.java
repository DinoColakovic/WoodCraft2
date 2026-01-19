package unze.ptf.woodcraft.woodcraft.io;

import java.util.List;

public class ProjectPackage {
    public int schemaVersion = 1;

    public DocumentDto document;

    public List<NodeDto> nodes;
    public List<EdgeDto> edges;
    public List<GuideDto> guides;
    public List<ShapeDto> shapes;
    public List<ManualShapeDto> manualShapes;
    public List<DimensionDto> dimensions;
    public List<MaterialDto> materials;

    public static class DocumentDto {
        public String name;
        public double widthCm;
        public double heightCm;
        public double kerfMm;
        public String unitSystem; // "CM" / "IN"
    }

    public static class NodeDto {
        public int id;
        public double xCm;
        public double yCm;
    }

    public static class EdgeDto {
        public int id;
        public int startNodeId;
        public int endNodeId;

        public Double controlStartXCm, controlStartYCm, controlEndXCm, controlEndYCm;
    }

    public static class GuideDto {
        public int id;
        public String orientation;
        public double positionCm;
    }

    public static class ShapeDto {
        public int id;
        public Integer materialId;
        public int quantity;
        public String nodeIds; // "1,2,3"
        public double areaCm2;
        public double perimeterCm;
    }

    public static class ManualShapeDto {
        public int id;
        public String points; // "x:y,x:y"
    }

    public static class DimensionDto {
        public int id;
        public String type;

        public double startXCm, startYCm;
        public double endXCm, endYCm;

        public double offsetXCm, offsetYCm;

        public Integer startNodeId;
        public Integer endNodeId;
    }

    public static class MaterialDto {
        public int id;

        public String name;
        public String type;

        public double sheetWidthCm;
        public double sheetHeightCm;
        public double sheetPrice;

        public double pricePerSquareMeter;
        public double pricePerLinearMeter;

        public String imagePath; // relativno u zipu: "assets/..."
        public String grainDirection;
        public double edgeBandingCostPerMeter;
    }
}
