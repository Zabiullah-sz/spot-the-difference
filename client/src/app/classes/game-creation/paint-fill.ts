// file: paint-fill.ts

import { Coordinates } from '@common/interfaces/general/coordinates';
import { AbstractTool } from './abstract-tool';

export class PaintFill extends AbstractTool {
    floodFill(coord: Coordinates, targetColor: Uint8ClampedArray, fillColor: string, context: CanvasRenderingContext2D): void {
        const stack = [coord];
        const visited: boolean[][] = Array(context.canvas.width)
            .fill([])
            .map(() => Array(context.canvas.height).fill(false));

        while (stack.length > 0) {
            const current = stack.pop()!;
            if (current.x < 0 || current.x >= context.canvas.width || current.y < 0 || current.y >= context.canvas.height) continue;

            const pixelColor = this.getPixelData(current, context).data;
            if (visited[current.x][current.y] || !this.colorsMatch(pixelColor, targetColor)) continue;

            this.setPixelData(current, fillColor, context);
            visited[current.x][current.y] = true;

            stack.push({ x: current.x + 1, y: current.y });
            stack.push({ x: current.x - 1, y: current.y });
            stack.push({ x: current.x, y: current.y + 1 });
            stack.push({ x: current.x, y: current.y - 1 });
        }
    }

    onMouseDownWithContext(coord: Coordinates, context: CanvasRenderingContext2D): void {
        const targetColor = this.getPixelData(coord, context).data;
        const fillColor = AbstractTool.color;
        if (!this.colorsMatch(targetColor, this.hexToRgba(fillColor))) {
            this.floodFill(coord, targetColor, fillColor, context);
        }
    }

    onMouseMove(coord: Coordinates, context: CanvasRenderingContext2D): void {
        // Do nothing for this tool on mouse move
    }
    private colorsMatch(a: Uint8ClampedArray | number[], b: Uint8ClampedArray | number[]): boolean {
        return a[0] === b[0] && a[1] === b[1] && a[2] === b[2] && a[3] === b[3];
    }
    private getPixelData(coord: Coordinates, context: CanvasRenderingContext2D): ImageData {
        return context.getImageData(coord.x, coord.y, 1, 1);
    }

    private setPixelData(coord: Coordinates, color: string, context: CanvasRenderingContext2D): void {
        const [r, g, b, a] = this.hexToRgba(color);
        context.fillStyle = `rgba(${r},${g},${b},${a})`;
        context.fillRect(coord.x, coord.y, 1, 1);
    }

    private hexToRgba(hex: string): [number, number, number, number] {
        const shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
        hex = hex.replace(shorthandRegex, (m, r, g, b) => r + r + g + g + b + b);

        const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
        return result ? [parseInt(result[1], 16), parseInt(result[2], 16), parseInt(result[3], 16), 255] : [0, 0, 0, 255];
    }
}
