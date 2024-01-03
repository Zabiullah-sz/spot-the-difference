import { DEFAULT_HEIGHT, DEFAULT_WIDTH } from '@app/constants/images-constants';
import { Coordinates } from '@common/interfaces/general/coordinates';
import { AbstractTool } from './abstract-tool';

export class Rectangle extends AbstractTool {
    private firstCornerCoord: Coordinates;
    private canvasSize = { x: DEFAULT_WIDTH, y: DEFAULT_HEIGHT };

    onMouseDown(coord: Coordinates, initialForeground: string): void {
        super.onMouseDown(coord, initialForeground);
        this.firstCornerCoord = coord;
    }

    onMouseMove(coord: Coordinates, context: CanvasRenderingContext2D): void {
        this.drawRectangle(coord, context);
        this.previousCoord = coord;
    }

    onKeyDown(key: string, context: CanvasRenderingContext2D): void {
        if (key === 'Shift') {
            this.drawSquare(this.previousCoord, context);
        }
    }

    onKeyUp(key: string, context: CanvasRenderingContext2D): void {
        if (key === 'Shift') {
            this.drawRectangle(this.previousCoord, context);
        }
    }

    private async drawRectangle(coord: Coordinates, context: CanvasRenderingContext2D) {
        await this.drawService.clearCanvas(this.canvasSize, context, this.initialForeground);
        context.strokeStyle = AbstractTool.color; // Utilisez strokeStyle pour les bordures
        context.lineWidth = 2; // Définissez la largeur des bordures selon vos besoins

        const width = Math.abs(coord.x - this.firstCornerCoord.x);
        const height = Math.abs(coord.y - this.firstCornerCoord.y);
        const x = Math.min(coord.x, this.firstCornerCoord.x);
        const y = Math.min(coord.y, this.firstCornerCoord.y);

        context.strokeRect(x, y, width, height);
    }
    private async drawSquare(coord: Coordinates, context: CanvasRenderingContext2D) {
        await this.drawService.clearCanvas(this.canvasSize, context, this.initialForeground);
        context.strokeStyle = AbstractTool.color;
        context.lineWidth = 2;

        const width = coord.x - this.firstCornerCoord.x;
        const height = coord.y - this.firstCornerCoord.y;

        // Calculez les coordonnées du coin supérieur gauche du carré
        const x = width > 0 ? this.firstCornerCoord.x : coord.x;
        const y = height > 0 ? this.firstCornerCoord.y : coord.y;

        // Calculez la longueur du côté du carré en fonction de la plus grande dimension (largeur ou hauteur)
        const sideLength = Math.max(Math.abs(width), Math.abs(height));

        context.strokeRect(x, y, sideLength, sideLength);
    }
}
