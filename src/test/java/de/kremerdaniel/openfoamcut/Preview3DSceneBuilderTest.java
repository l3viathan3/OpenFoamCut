package de.kremerdaniel.openfoamcut;

import de.kremerdaniel.openfoamcut.bo.CutOutline;
import de.kremerdaniel.openfoamcut.bo.Line;
import de.kremerdaniel.openfoamcut.bo.Point;
import de.kremerdaniel.openfoamcut.bo.Side;
import de.kremerdaniel.openfoamcut.bo.SyncedPoints;
import de.kremerdaniel.openfoamcut.cutting.GCodeConverter;
import de.kremerdaniel.openfoamcut.cutting.GCodeResult;
import de.kremerdaniel.openfoamcut.model.RuntimeModelManager;
import de.kremerdaniel.openfoamcut.model.StateManager;
import de.kremerdaniel.openfoamcut.preview.Preview3DScene;
import de.kremerdaniel.openfoamcut.preview.Preview3DSceneBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for building preview geometry from the generated cut result.
 */
class Preview3DSceneBuilderTest {

    @Test
    void buildsSceneWithFoamOffsetsAndClosedSolidFaces() {
        StateManager stateManager = StateManager.getInstance();
        RuntimeModelManager runtimeModelManager = RuntimeModelManager.getInstance();

        stateManager.setFoamWidth(200);
        stateManager.setFoamDepth(120);
        stateManager.setFoamHeight(40);
        stateManager.setFoamOffsetX(15);
        stateManager.setFoamOffsetLeft(25);
        stateManager.setFoamOffsetRight(35);
        stateManager.setLeftOffsetX(0);
        stateManager.setRightOffsetX(0);
        stateManager.setLeftOffsetY(0);
        stateManager.setRightOffsetY(0);
        stateManager.setLeftRotation(0);
        stateManager.setRightRotation(0);
        stateManager.setLeftScale(1.0);
        stateManager.setRightScale(1.0);
        stateManager.getCutOrder().clear();

        CutOutline outline = new CutOutline(List.of(
            new Line(new Point(0, 0), new Point(100, 0)),
            new Line(new Point(100, 0), new Point(100, 20)),
            new Line(new Point(100, 20), new Point(0, 20)),
            new Line(new Point(0, 20), new Point(0, 0))
        ), true);

        runtimeModelManager.setOutline(Side.LEFT, outline);
        runtimeModelManager.setOutline(Side.RIGHT, outline);

        stateManager.getCutOrder().add(new SyncedPoints(new Point(15, 0), new Point(15, 0)));
        stateManager.getCutOrder().add(new SyncedPoints(new Point(115, 0), new Point(115, 0)));
        stateManager.getCutOrder().add(new SyncedPoints(new Point(115, 20), new Point(115, 20)));
        stateManager.getCutOrder().add(new SyncedPoints(new Point(15, 20), new Point(15, 20)));

        GCodeResult result = new GCodeConverter().generateGCodeFromState();
        Preview3DScene scene = new Preview3DSceneBuilder().buildScene(result, stateManager);

        assertFalse(scene.getMeshEdges().isEmpty());
        assertFalse(scene.getSolidFaces().isEmpty());
        assertEquals(0.0, scene.getBounds().getMin().getX(), 0.001);
        assertEquals(25.0 + 200.0 + 35.0, scene.getBounds().getMax().getX(), 0.001);
        assertTrue(scene.getBounds().getMax().getY() >= 15.0 + 120.0);
    }

    @Test
    void reducesMeshEdgeCountWhenMeshStepIncreases() {
        StateManager stateManager = StateManager.getInstance();
        RuntimeModelManager runtimeModelManager = RuntimeModelManager.getInstance();

        stateManager.setFoamWidth(200);
        stateManager.setFoamDepth(120);
        stateManager.setFoamHeight(40);
        stateManager.setFoamOffsetX(15);
        stateManager.setFoamOffsetLeft(25);
        stateManager.setFoamOffsetRight(35);
        stateManager.setLeftOffsetX(0);
        stateManager.setRightOffsetX(0);
        stateManager.setLeftOffsetY(0);
        stateManager.setRightOffsetY(0);
        stateManager.setLeftRotation(0);
        stateManager.setRightRotation(0);
        stateManager.setLeftScale(1.0);
        stateManager.setRightScale(1.0);
        stateManager.getCutOrder().clear();

        CutOutline outline = new CutOutline(List.of(
            new Line(new Point(0, 0), new Point(100, 0)),
            new Line(new Point(100, 0), new Point(100, 20)),
            new Line(new Point(100, 20), new Point(0, 20)),
            new Line(new Point(0, 20), new Point(0, 0))
        ), true);

        runtimeModelManager.setOutline(Side.LEFT, outline);
        runtimeModelManager.setOutline(Side.RIGHT, outline);

        stateManager.getCutOrder().add(new SyncedPoints(new Point(15, 0), new Point(15, 0)));
        stateManager.getCutOrder().add(new SyncedPoints(new Point(115, 0), new Point(115, 0)));
        stateManager.getCutOrder().add(new SyncedPoints(new Point(115, 20), new Point(115, 20)));
        stateManager.getCutOrder().add(new SyncedPoints(new Point(15, 20), new Point(15, 20)));

        GCodeResult result = new GCodeConverter().generateGCodeFromState();
        Preview3DScene denseScene = new Preview3DSceneBuilder().buildScene(result, stateManager, 1.0);
        Preview3DScene sparseScene = new Preview3DSceneBuilder().buildScene(result, stateManager, 25.0);

        assertTrue(denseScene.getMeshEdges().size() > sparseScene.getMeshEdges().size());
    }
}