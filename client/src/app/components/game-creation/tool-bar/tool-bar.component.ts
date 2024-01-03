import { Component, ElementRef, HostListener, OnInit, ViewChild } from '@angular/core';
import { AbstractTool } from '@app/classes/game-creation/abstract-tool';
import { Circle } from '@app/classes/game-creation/circle';
import { Ellipse } from '@app/classes/game-creation/ellipse';
import { Eraser } from '@app/classes/game-creation/eraser';
import { PaintFill } from '@app/classes/game-creation/paint-fill';
import { Pencil } from '@app/classes/game-creation/pencil';
import { Rectangle } from '@app/classes/game-creation/rectangle';
import { TOOL_DEFAULT_SIZE, TOOL_MAX_SIZE, TOOL_MIN_SIZE } from '@app/constants/drawing-tools-constants';
import { DrawService } from '@app/services/game-creation/foreground/draw.service';
import { ForegroundDataService } from '@app/services/game-creation/foreground/foreground-data.service';
import { SelectedToolService } from '@app/services/game-creation/foreground/selected-tool.service';
import { ImageIndex } from '@common/enums/game-creation/image-index';
import { Square } from '@app/classes/game-creation/square';

@Component({
    selector: 'app-tool-bar',
    templateUrl: './tool-bar.component.html',
    styleUrls: ['./tool-bar.component.scss'],
})
export class ToolBarComponent implements OnInit {
    @ViewChild('sizeInput') sizeInput: ElementRef;

    toolSize: number;
    pencil = new Pencil(this.drawService);
    rectangle = new Rectangle(this.drawService);
    square = new Square(this.drawService);
    circle = new Circle(this.drawService);
    eraser = new Eraser(this.drawService);
    ellipse = new Ellipse(this.drawService);
    paintFill = new PaintFill(this.drawService);
    canvas: HTMLCanvasElement;
    ctx: CanvasRenderingContext2D;
    isDrawing: boolean = false;
    startX: number;
    startY: number;

    constructor(
        private selectedToolService: SelectedToolService,
        private drawService: DrawService,
        private foregroundDataService: ForegroundDataService,
    ) {}

    get selectedTool(): AbstractTool {
        return this.selectedToolService.selectedTool;
    }

    get undoIsPossible(): boolean {
        return this.foregroundDataService.undoIsPossible;
    }

    get redoIsPossible(): boolean {
        return this.foregroundDataService.redoIsPossible;
    }

    get bothImagesIndex() {
        return ImageIndex.Both;
    }

    get minSize() {
        return TOOL_MIN_SIZE;
    }

    get maxSize() {
        return TOOL_MAX_SIZE;
    }

    @HostListener('window:keydown', ['$event'])
    onKeyDown(e: KeyboardEvent): void {
        if (e.ctrlKey && e.key.toLowerCase() === 'z') {
            if (e.shiftKey) {
                this.redo();
            } else {
                this.undo();
            }
        }
    }

    ngOnInit() {
        this.selectPencil();
        this.toolSize = TOOL_DEFAULT_SIZE;
        this.setLineSize();
        this.setColor('#000');
        this.canvas = document.getElementById('canvas') as HTMLCanvasElement;
        if (!this.canvas) {
            // eslint-disable-next-line no-console
            console.error('Canvas element not found.');
            return;
        }

        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        this.ctx = this.canvas.getContext('2d')!;

        if (!this.ctx) {
            // eslint-disable-next-line no-console
            console.error('Canvas 2D context is not available.');
        } else {
            this.addMouseListeners();
        }
    }

    setLineSize(): void {
        this.validateSizeInput();
        AbstractTool.size = this.toolSize;
    }

    setColor(color: string): void {
        AbstractTool.color = color;
    }

    selectPencil(): void {
        this.selectedToolService.selectedTool = this.pencil;
    }

    selectEraser(): void {
        this.selectedToolService.selectedTool = this.eraser;
    }

    selectRectangle(): void {
        this.selectedToolService.selectedTool = this.rectangle;
    }
    selectPaintFill(): void {
        this.selectedToolService.selectedTool = this.paintFill;
    }
    selectSquare(): void {
        this.selectedToolService.selectedTool = this.square;
    }
    selectCircle(): void {
        this.selectedToolService.selectedTool = this.circle;
    }
    selectEllipse(): void {
        this.selectedToolService.selectedTool = this.ellipse;
    }
    undo(): void {
        this.foregroundDataService.undo();
    }

    redo(): void {
        this.foregroundDataService.redo();
    }

    private validateSizeInput() {
        if (this.toolSize < TOOL_MIN_SIZE) {
            this.sizeInput.nativeElement.value = TOOL_MIN_SIZE;
            this.toolSize = TOOL_MIN_SIZE;
        } else if (this.toolSize > TOOL_MAX_SIZE) {
            this.sizeInput.nativeElement.value = TOOL_MAX_SIZE;
            this.toolSize = TOOL_MAX_SIZE;
        }
    }

    private addMouseListeners() {
        this.canvas.addEventListener('mousedown', (e) => this.onMouseDown(e));
        this.canvas.addEventListener('mousemove', (e) => this.onMouseMove(e));
        this.canvas.addEventListener('mouseup', () => this.onMouseUp());
    }

    private onMouseDown(e: MouseEvent) {
        this.isDrawing = true;
        this.startX = e.clientX - this.canvas.getBoundingClientRect().left;
        this.startY = e.clientY - this.canvas.getBoundingClientRect().top;
    }

    private onMouseMove(e: MouseEvent) {
        if (!this.isDrawing) return;

        const currentX = e.clientX - this.canvas.getBoundingClientRect().left;
        const currentY = e.clientY - this.canvas.getBoundingClientRect().top;

        // Clear the canvas
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        // Draw the rectangle
        const width = currentX - this.startX;
        const height = currentY - this.startY;
        this.ctx.strokeStyle = AbstractTool.color;
        this.ctx.lineWidth = AbstractTool.size;
        this.ctx.strokeRect(this.startX, this.startY, width, height);
    }

    private onMouseUp() {
        this.isDrawing = false;
    }
}
