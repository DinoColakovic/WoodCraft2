package unze.ptf.woodcraft.woodcraft.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import unze.ptf.woodcraft.woodcraft.model.Document;
import unze.ptf.woodcraft.woodcraft.model.Edge;
import unze.ptf.woodcraft.woodcraft.model.NodePoint;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfExportService {
    private static final float POINTS_PER_CM = 28.3465f;
    private static final float MARGIN_CM = 1.5f;

    public void export(Document document, List<NodePoint> nodes, List<Edge> edges, File target) throws IOException {
        try (PDDocument pdf = new PDDocument()) {
            float widthPt = (float) document.getWidthCm() * POINTS_PER_CM + (MARGIN_CM * 2 * POINTS_PER_CM);
            float heightPt = (float) document.getHeightCm() * POINTS_PER_CM + (MARGIN_CM * 2 * POINTS_PER_CM);
            PDPage page = new PDPage(new PDRectangle(widthPt, heightPt));
            pdf.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                float originX = MARGIN_CM * POINTS_PER_CM;
                float originY = MARGIN_CM * POINTS_PER_CM;
                float canvasWidth = (float) document.getWidthCm() * POINTS_PER_CM;
                float canvasHeight = (float) document.getHeightCm() * POINTS_PER_CM;

                content.setLineWidth(1);
                content.setStrokingColor(120);
                content.addRect(originX, originY, canvasWidth, canvasHeight);
                content.stroke();

                content.setLineWidth(1.2f);
                content.setStrokingColor(40);
                for (Edge edge : edges) {
                    NodePoint start = findNode(nodes, edge.getStartNodeId());
                    NodePoint end = findNode(nodes, edge.getEndNodeId());
                    if (start == null || end == null) {
                        continue;
                    }
                    float x1 = originX + (float) start.getXCm() * POINTS_PER_CM;
                    float y1 = originY + (float) start.getYCm() * POINTS_PER_CM;
                    float x2 = originX + (float) end.getXCm() * POINTS_PER_CM;
                    float y2 = originY + (float) end.getYCm() * POINTS_PER_CM;
                    content.moveTo(x1, y1);
                    content.lineTo(x2, y2);
                    content.stroke();
                }
            }
            pdf.save(target);
        }
    }

    private NodePoint findNode(List<NodePoint> nodes, int id) {
        for (NodePoint node : nodes) {
            if (node.getId() == id) {
                return node;
            }
        }
        return null;
    }
}
