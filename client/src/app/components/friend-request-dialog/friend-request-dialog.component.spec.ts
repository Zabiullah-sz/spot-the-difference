import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FriendRequestDialogComponent } from './friend-request-dialog.component';

describe('FriendRequestDialogComponent', () => {
  let component: FriendRequestDialogComponent;
  let fixture: ComponentFixture<FriendRequestDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ FriendRequestDialogComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FriendRequestDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
