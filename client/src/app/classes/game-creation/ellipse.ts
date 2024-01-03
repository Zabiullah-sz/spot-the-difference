import { DEFAULT_HEIGHT, DEFAULT_WIDTH } from '@app/constants/images-constants';
import { Coordinates } from '@common/interfaces/general/coordinates';
import { AbstractTool } from './abstract-tool';

export class Ellipse extends AbstractTool {
    private centerCoord: Coordinates;
    private canvasSize = { x: DEFAULT_WIDTH, y: DEFAULT_HEIGHT };

    onMouseDown(coord: Coordinates, initialForeground: string): void {
        super.onMouseDown(coord, initialForeground);
        this.centerCoord = coord;
    }

    onMouseMove(coord: Coordinates, context: CanvasRenderingContext2D): void {
        this.drawEllipse(coord, context);
        this.previousCoord = coord;
    }

    onKeyDown(key: string, context: CanvasRenderingContext2D): void {
        if (key === 'Shift') {
            this.drawEllipse(this.previousCoord, context);
        }
    }

    onKeyUp(key: string, context: CanvasRenderingContext2D): void {
        if (key === 'Shift') {
            this.drawEllipse(this.previousCoord, context);
        }
    }

    private async drawEllipse(coord: Coordinates, context: CanvasRenderingContext2D) {
        await this.drawService.clearCanvas(this.canvasSize, context, this.initialForeground);
        context.strokeStyle = AbstractTool.color; // Utilisez strokeStyle pour les bordures
        context.lineWidth = 2; // Définissez la largeur des bordures selon vos besoins

        const radiusX = this.calculateRadiusX(this.centerCoord, coord);
        const radiusY = this.calculateRadiusY(this.centerCoord, coord);

        context.beginPath();
        context.ellipse(this.centerCoord.x, this.centerCoord.y, radiusX, radiusY, 0, 0, Math.PI * 2);
        context.stroke();
    }

    private calculateRadiusX(center: Coordinates, point: Coordinates): number {
        return Math.abs(point.x - center.x);
    }

    private calculateRadiusY(center: Coordinates, point: Coordinates): number {
        return Math.abs(point.y - center.y);
    }
}
