import { ComponentFactoryResolver, Injectable, Injector, ApplicationRef, EmbeddedViewRef } from '@angular/core';

@Injectable({
    providedIn: 'root',
})
export class WindowService {
    constructor(private componentFactoryResolver: ComponentFactoryResolver, private appRef: ApplicationRef, private injector: Injector) {}

    openComponentInNewWindow(component: unknown) {
        // Create a new window
        const winRef = window.open('', '_blank');
        if (!winRef) return;
        this.copyStyles(window.document, winRef.document);

        // Resolve the component
        const componentRef = this.componentFactoryResolver.resolveComponentFactory(component as any).create(this.injector);

        this.appRef.attachView(componentRef.hostView);

        // Append component to the new window
        const domElem = (componentRef.hostView as EmbeddedViewRef<unknown>).rootNodes[0] as HTMLElement;

        winRef?.document.body.appendChild(domElem);

        // Ensure the new window is closed when Angular disposes of the component
        winRef?.addEventListener('beforeunload', () => {
            this.appRef.detachView(componentRef.hostView);
        });
        //listen for window close
        //winRef?.addEventListener('unload', () => {
        //    //onDestroy();
        //    componentRef.destroy();
        //});
        //close window when component is destroyed

        return winRef;
    }
    private copyStyles(sourceDoc: Document, targetDoc: Document) {
        Array.from(sourceDoc.querySelectorAll('link[rel="stylesheet"], style')).forEach((linkOrStyle) => {
            const newNode = targetDoc.createElement(linkOrStyle.tagName);

            Array.from(linkOrStyle.attributes).forEach((attr) => {
                newNode.setAttribute(attr.name, attr.value);
            });

            if (linkOrStyle.tagName === 'STYLE') {
                newNode.appendChild(targetDoc.createTextNode(linkOrStyle.innerHTML));
            }

            targetDoc.head.appendChild(newNode);
        });
    }
}
