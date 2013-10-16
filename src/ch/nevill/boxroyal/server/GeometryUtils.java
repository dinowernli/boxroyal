package ch.nevill.boxroyal.server;

import ch.nevill.boxroyal.proto.Direction;
import ch.nevill.boxroyal.proto.Point;
import ch.nevill.boxroyal.proto.Point.Builder;
import ch.nevill.boxroyal.proto.Size;

public class GeometryUtils {

  static Point applyDirection(Point point, Direction direction) {
    Builder builder = point.toBuilder();
    switch (direction.getNumber()) {
      case Direction.NORTH_VALUE:
        builder.setY(point.getY() + 1);
        break;
      case Direction.EAST_VALUE:
        builder.setX(point.getX() + 1);
        break;
      case Direction.SOUTH_VALUE:
        builder.setY(point.getY() - 1);
        break;
      case Direction.WEST_VALUE:
        builder.setX(point.getX() - 1);
        break;
      default:
        throw new IllegalArgumentException();
    }
    return builder.build();
  }

  static boolean pointInArea(Point point, Size area) {
    return point.getX() >= 0
        && point.getY() >= 0
        && point.getX() < area.getWidth()
        && point.getY() < area.getHeight();
  }

  static boolean pointInPath(Point start, Direction direction, Point target) {
    if (start.getX() != target.getX() && start.getY() != target.getY()) {
      return false;
    }
    if (start.getX() == target.getX() && start.getY() == target.getY()) {
      return true;
    }

    switch (direction.getNumber()) {
      case Direction.NORTH_VALUE:
        return target.getY() > start.getY();
      case Direction.EAST_VALUE:
        return target.getX() > start.getX();
      case Direction.SOUTH_VALUE:
        return target.getY() < start.getY();
      case Direction.WEST_VALUE:
        return target.getX() < start.getX();
      default:
        throw new IllegalArgumentException();
    }
  }

}
