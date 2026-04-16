package de.kremerdaniel.openfoamcut.bo;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents synchronized cut lines for left and right sides
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncedCutLines {
    private Point startLeft = new Point();
    private Point endLeft = new Point();
    private Point startRight = new Point();
    private Point endRight = new Point();
    private List<Line> linesLeft = new ArrayList<>();
    private List<Line> linesRight = new ArrayList<>();
}