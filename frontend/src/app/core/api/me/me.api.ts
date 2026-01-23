import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { ApiResponse } from '../types/api-response';
import { MeResponse, UpdateProfileRequest } from './me.models';

@Injectable({ providedIn: 'root' })
export class MeApi {
  private readonly http = inject(HttpClient);

  me(): Observable<MeResponse> {
    return this.http.get<ApiResponse<MeResponse>>('/api/me').pipe(
      map((res) => res.data!)
    );
  }

  updateProfile(req: UpdateProfileRequest): Observable<MeResponse> {
    return this.http.put<ApiResponse<MeResponse>>('/api/me/profile', req).pipe(
      map((res) => res.data!)
    );
  }
}
