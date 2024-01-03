import { DEFAULT_HEIGHT, DEFAULT_WIDTH } from '@app/constants/images-constants';
import { Coordinates } from '@common/interfaces/general/coordinates';
import { AbstractTool } from './abstract-tool';

export class Circle extends AbstractTool {
    private centerCoord: Coordinates;
    private canvasSize = { x: DEFAULT_WIDTH, y: DEFAULT_HEIGHT };
    private isShiftKeyHeld = false;

    onKeyDown(key: string, context: CanvasRenderingContext2D): void {
        if (key === 'Shift') {
            this.drawEllipse(this.previousCoord, context);
        }
    }

    onKeyUp(key: string, context: CanvasRenderingContext2D): void {
        if (key === 'Shift') {
            this.drawCircle(this.previousCoord, context);
        }
    }

    onMouseDown(coord: Coordinates, initialForeground: string): void {
        super.onMouseDown(coord, initialForeground);
        this.centerCoord = coord;
    }

    onMouseMove(coord: Coordinates, context: CanvasRenderingContext2D): void {
        this.drawCircleOrEllipse(coord, context);
        this.previousCoord = coord;
    }

    private async drawCircle(coord: Coordinates, context: CanvasRenderingContext2D) {
        await this.drawService.clearCanvas(this.canvasSize, context, this.initialForeground);
        context.strokeStyle = AbstractTool.color;
        context.lineWidth = 2;

        const radius = this.calculateRadius(this.centerCoord, coord);

        context.beginPath();
        context.arc(this.centerCoord.x, this.centerCoord.y, radius, 0, Math.PI * 2);
        context.stroke();
    }

    private async drawEllipse(coord: Coordinates, context: CanvasRenderingContext2D) {
        await this.drawService.clearCanvas(this.canvasSize, context, this.initialForeground);
        context.strokeStyle = AbstractTool.color;
        context.lineWidth = 2;

        const radiusX = this.calculateRadiusX(this.centerCoord, coord);
        const radiusY = this.calculateRadiusY(this.centerCoord, coord);

        context.beginPath();
        context.ellipse(this.centerCoord.x, this.centerCoord.y, radiusX, radiusY, 0, 0, Math.PI * 2);
        context.stroke();
    }

    private drawCircleOrEllipse(coord: Coordinates, context: CanvasRenderingContext2D) {
        if (this.isShiftKeyHeld) {
            this.drawEllipse(coord, context);
        } else {
            this.drawCircle(coord, context);
        }
    }

    private calculateRadius(center: Coordinates, point: Coordinates): number {
        const deltaX = point.x - center.x;
        const deltaY = point.y - center.y;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    private calculateRadiusX(center: Coordinates, point: Coordinates): number {
        return Math.abs(point.x - center.x);
    }

    private calculateRadiusY(center: Coordinates, point: Coordinates): number {
        return Math.abs(point.y - center.y);
    }
}
