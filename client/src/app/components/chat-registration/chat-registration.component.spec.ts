import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChatRegistrationComponent } from './chat-registration.component';

describe('ChatRegistrationComponent', () => {
  let component: ChatRegistrationComponent;
  let fixture: ComponentFixture<ChatRegistrationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ChatRegistrationComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChatRegistrationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
